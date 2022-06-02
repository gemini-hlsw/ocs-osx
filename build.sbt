import NativePackagerHelper._

name in ThisBuild := "ocs-osx"

Global / onChangedBuildSource := ReloadOnSourceChanges

scalaVersion in ThisBuild := "2.11.11"

organization := "edu.gemini"

description := "MacOS builder of legacy OT apps"

homepage := Some(url("https://github.com/gemini-hlsw/ocs-osx"))

licenses := Seq("BSD 3-Clause License" -> url("https://opensource.org/licenses/BSD-3-Clause"))

addCommandAlias("pitDmg", "; pit/clean; pit/notarizedDmgFormat:packageBin")

val root = project
  .aggregate(pit)

lazy val pit = project
  .in(file("pit"))
  .enablePlugins(NotarizedDmgPlugin)
  .settings(
    version := "2020102.1.0", // Same version as defined by the pitlauncher bundle
    libraryDependencies ++= Seq(
      "edu.gemini.ocs" %% "edu-gemini-pit-launcher" % version.value,
      "org.osgi" % "org.osgi.core" % "4.2.0",
      "javax.xml.bind" % "jaxb-api" % "2.3.1",
      "org.glassfish.jaxb" % "jaxb-runtime" % "2.3.1",
      "io.argonaut" %% "argonaut" % "6.2.6"
    ),
    appName := "Gemini PIT",
    maintainer in Universal := "Gemini Software Group",
    mappings in (Compile, packageDoc) := Seq(),
    mainClass in Compile := Some("edu.gemini.pit.launcher.PITLauncher"),
    id := "edu.gemini.pit",
    // Signature of the signing certifcate
    signatureID := "T87F4ZD75E",
    // Hash of the certifacte, need for signig
    certificateHash := "60464455CE099B3293643BC0021D1F25D2F52A59",
    notarizationUID := "its@gemini.edu",
    icon := "PIT.icns",
    // Copy  the JRE
    mappings in Universal ++= {
      val jresDir = Path.userHome / ".jres13"
      val osxJre  = jresDir.toPath.resolve("jre")
      directory(osxJre.toFile).map { j =>
        j._1 -> j._2
      }
    },
    mappings in Universal := {
      // Move the launcher to the MacOS dir
      val universalMappings = (mappings in Universal).value
      val filtered = universalMappings.map {
        case (n, name) if name.startsWith("bin") => (n, name.replace("bin", "MacOS"))
        case x                                   => x
      }
      filtered
    },
    mappings in Universal ++= (resources in Compile).value.map { r =>
      // Copy the icon to Resources
      r -> ("Resources/" + r.getName)
    },
    // Custom command to support the icon on the dock
    bashScriptExtraDefines += s"""addJava "-Xdock:icon=$${app_home}/../Resources/${icon.value}"""",
    // Custom command to support the name on the dock
    bashScriptExtraDefines += s"""addJava "-Xdock:name=${appName.value}"""",
    javaOptions in Universal ++= Seq(
      // Don't check the java version or it will try to use the locally built one
      "-no-version-check",
      // Enforce the embedded jre
      "-java-home ${app_home}/../jre/Contents/Home",
      "-Dedu.gemini.pit.test=false",
      "-J-Xmx512M",
      "-Dedu.gemini.ui.workspace.impl.Workspace.fonts.shrunk=true",
      "-Dfile.encoding=UTF-8",
      "-Duser.language=en",
      "-Duser.country=US",
      "-Dedu.gemini.ags.host=gnauxodb.gemini.edu",
      "-Dedu.gemini.ags.port=8443"
    ),
    // Don't create launchers for Windows
    makeBatScripts := Seq.empty
  )
