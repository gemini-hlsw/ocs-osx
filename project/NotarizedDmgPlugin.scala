import sbt._
import sbt.Keys._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaAppPackaging
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import java.nio.file.{ FileSystems, FileVisitResult, Files, Path, Paths, SimpleFileVisitor }
import java.nio.file.attribute.BasicFileAttributes
import sys.process._

object NotarizedDmgPlugin extends AutoPlugin {

  class SbtLogger(logger: Logger) {
    def info(s: String): Unit =
      logger.info(s)
    def error(s: String): Unit =
      logger.error(s)
  }
  // enable this plugin when all requirements are fullfilled (in this case JavaAppPackaging has been loaded)
  override def trigger = AllRequirements

  // depend on JavaAppPackaging
  override def requires = JavaAppPackaging

  // scope gets automatically imported
  object autoImport {
    val NotarizedDmgFormat = config("notarizedDmgFormat")
    val logger             = taskKey[SbtLogger]("Holds instance of SomeClassThatNeedsLogger")
    val id                 = taskKey[String]("Apple bundle id")
    val appName            = taskKey[String]("App public name")
    val signatureID        = taskKey[String]("Apple signature ID")
    val certificateHash    = taskKey[String]("Apple Certificate Hash")
    val notarizationUID    = taskKey[String]("Notarization UID")
    val icon               = taskKey[String]("Icon")
  }

  import autoImport._

  // project settings applied when this plugin is activated
  override def projectSettings =
    inConfig(NotarizedDmgFormat)(
      Seq(
        // define a custom target directory
        target := target.value / "dmg",
        name := name.value,
        executableScriptName := executableScriptName.value,
        appName := appName.value,
        version := version.value,
        mainClass := (mainClass in Compile).value,
        mappings := (mappings in Universal).value,
        packageName := (packageName in Universal).value,
        logger := new SbtLogger(streams.value.log),
        // implementing the packageBin task
        packageBin := {
          val t               = target.value
          val bundleId        = id.value
          val certHash        = certificateHash.value
          val applicationName = appName.value
          val versionY        = version.value
          val executable      = executableScriptName.value
          val projectName     = name.value.replaceAll(" ", "_")
          val notUID          = notarizationUID.value
          val iconFile        = new File("Resources", icon.value)
          val dmgName         = s"${projectName}_$versionY"
          val dmg             = new File(t, (dmgName + ".dmg"))
          val entitlements =
            Paths.get(getClass.getClassLoader.getResource("entitlements.plist").toURI).toFile
          val log = logger.value
          log.info(s"Creating dmg $applicationName $versionY")
          val fullName = s"$applicationName $versionY.app"
          val dmgPath  = new File(t, fullName)
          if (dmg.exists) IO.delete(dmg)

          if (!t.isDirectory) IO.createDirectory(t)
          IO.delete(dmgPath)
          IO.createDirectory(dmgPath)
          val sizeBytes =
            mappings.value.map(_._1).filterNot(_.isDirectory).map(_.length).sum
          // We should give ourselves a buffer....
          val neededMegabytes = math.ceil((sizeBytes * 1.5) / (1024 * 1024)).toLong

          // Create the DMG file:
          val contentPath = dmgPath / "Contents"

          IO.write(new File(contentPath, "PkgInfo"), "APPL????")
          val pList = InfoPlist(
            executable,
            bundleId,
            applicationName,
            version.value,
            version.value,
            mainClass.value.getOrElse(sys.error("Sbt main class required")),
            Some(iconFile)
          )
          pList.write(new File(contentPath, "Info.plist").getAbsolutePath)

          // Now copy the files in
          val m2 = mappings.value.map { case (f, p) => f -> (contentPath / p) }
          IO.copy(m2)
          // Update for permissions
          for {
            (from, to) <- m2
            if from.canExecute()
          } to.setExecutable(true, true)

          val identities = Seq("security", "find-identity", "-v", "-p", "codesigning")
          val canSign    = identities.lineStream.exists(_.contains(signatureID.value))
          if (canSign) {
            log.info("Signing code with developer ID")
            // Sign each jar and dylib file
            Files.walkFileTree(
              dmgPath.toPath,
              new SimpleFileVisitor[Path]() {
                override def visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult = {
                  Seq("codesign",
                      "-f",
                      "--options",
                      "runtime",
                      "--timestamp",
                      "--entitlements",
                      entitlements.getAbsolutePath(),
                      "-v",
                      "-s",
                      certHash,
                      file.toFile.getAbsolutePath).!
                  FileVisitResult.CONTINUE
                }
              }
            )
          }

          val volname = "%s_%s".format(projectName, versionY)
          val dmgname = volname + ".dmg"
          val dest    = new File(t, dmgname)
          if (dest.exists) IO.delete(dest)
          val args = Seq("hdiutil",
                         "create",
                         "-size",
                         s"${neededMegabytes}m",
                         "-srcfolder",
                         dmgPath.getAbsolutePath,
                         "-volname",
                         volname,
                         dest.getAbsolutePath)
          val result = args.!
          if (result != 0) {
            log.error("*** " + args.mkString(" "))
            log.error("*** HDIUTIL RETURNED " + result)
          }

          // Delete mount point
          IO.delete(dmgPath)
          if (canSign) {
            Seq("codesign",
                "-f",
                "--options",
                "runtime",
                "--timestamp",
                "-v",
                "-s",
                certHash,
                dest.getAbsolutePath).lineStream

          }
          val notarizeCmd = Seq("xcrun",
                                "altool",
                                "--list-providers",
                                "-u",
                                notUID,
                                "-p",
                                """@keychain:AC_PASSWORD""")
          val canNotarize = notarizeCmd.lineStream.filter(_.contains("Error")).isEmpty
          if (canNotarize) {
            val notarize = Seq(
              "xcrun",
              "altool",
              "--notarize-app",
              "--primary-bundle-id",
              bundleId,
              "-u",
              notUID,
              "-p",
              """@keychain:AC_PASSWORD""",
              "-f",
              dmg.getAbsolutePath
            )
            notarize.lineStream.foreach(log.info(_))
          }

          dmg
        }
      )
    )
}
