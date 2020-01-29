import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys.packageName
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._

object NotarizedDmgPlugin extends AutoPlugin {

  class SbtLogger(logger: Logger) {
    def info(s: String): Unit = {
      logger.info(s)
    }
  }
  // enable this plugin when all requirements are fullfilled (in this case JavaAppPackaging has been loaded)
  override def trigger = AllRequirements

  // depend on JavaAppPackaging
  override def requires = JavaAppPackaging

  // scope gets automatically imported
  object autoImport {
    val NotarizedDmgFormat = config("notarizedDmgFormat")
    val logger = taskKey[SbtLogger]("Holds instance of SomeClassThatNeedsLogger")
    val id = taskKey[String]("Apple bundle id")
  }

  import autoImport._

  // project settings applied when this plugin is activated
  override def projectSettings = inConfig(NotarizedDmgFormat)(Seq(
    // define a custom target directory
    target := target.value / "dmg",
    name := (name in Universal).value,
    mainClass := (mainClass in Compile).value,
    mappings := (mappings in Universal).value,
    packageName := (packageName in Universal).value,
    logger := new SbtLogger(streams.value.log),

    // implementing the packageBin task
    packageBin := {
      val t = target.value
      val dmgName = name.value.replaceAll(" ", "_")
      val dmg = new File(t, (dmgName + ".dmg"))
      val log = logger.value
      log.info(s"Creating dmg $dmg")
      if (dmg.exists) IO.delete(dmg)

      if (!t.isDirectory) IO.createDirectory(t)
      val sizeBytes =
        mappings.value.map(_._1).filterNot(_.isDirectory).map(_.length).sum
      // We should give ourselves a buffer....
      val neededMegabytes = math.ceil((sizeBytes * 1.05) / (1024 * 1024)).toLong

      // Create the DMG file:
      sys.process.Process.apply(
          command = Seq[String]("hdiutil", "create", "-megabytes", "%d" format neededMegabytes, "-fs", "HFS+", "-volname",dmgName, dmgName),
          cwd = t
        ).! match {
        case 0 => ()
        case n => sys.error("Error creating dmg: " + dmg + ". Exit code " + n)
      }

      // Now mount the DMG.
      val mountPoint = (t / dmgName)
      if (!mountPoint.isDirectory) IO.createDirectory(mountPoint)
      val mountedPath = mountPoint.getAbsolutePath
      sys.process.Process(Seq[String]("hdiutil", "attach", dmg.getAbsolutePath, "-readwrite", "-mountpoint", mountedPath), t).! match {
        case 0 => ()
        case n => sys.error("Unable to mount dmg: " + dmg + ". Exit code " + n)
      }

      IO.write(new File(mountedPath, "PkgInfo"), "APPL????")
      val regex = """(\w*).*""".r
      // regex.
      val pList = InfoPlist(id.value, name.value, version.value, version.value, mainClass.value.getOrElse(sys.error("Sbt main class required")), Map.empty, Seq.empty, None, "")
      IO.write(new File(mountedPath, "Info.plist"), pList.xml.toString)
      log.info(pList.xml.toString)

      // Now copy the files in
      val m2 = mappings.value map { case (f, p) => f -> (mountPoint / p) }
      IO.copy(m2)
      // Update for permissions
      for {
        (from, to) <- m2
        if from.canExecute()
      } to.setExecutable(true, true)

      // Now unmount
      sys.process.Process(Seq("hdiutil", "detach", mountedPath), target.value).! match {
        case 0 => ()
        case n =>
          sys.error("Unable to dismount dmg: " + dmg + ". Exit code " + n)
      }
      // Delete mount point
      IO.delete(mountPoint)
      dmg
    }
  ))
}
