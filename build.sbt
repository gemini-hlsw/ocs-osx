import NativePackagerHelper._

name in ThisBuild := "ocs-osx"

Global / onChangedBuildSource := ReloadOnSourceChanges

scalaVersion in ThisBuild := "2.11.11"

organization := "edu.gemini"

description := "MacOS builder of legacy OT apps"

homepage := Some(url("https://github.com/gemini-hlsw/ocs-osx"))

licenses := Seq("BSD 3-Clause License" -> url("https://opensource.org/licenses/BSD-3-Clause"))

addCommandAlias("pitDmg", "; pit/clean; pit/universal:packageOsxDmg")

val root = project
  .aggregate(pit)

lazy val pit = project
  .in(file("pit"))
  .enablePlugins(NotarizedDmgPlugin)
  .settings(
    version := "2020102.1.0",
    libraryDependencies ++= Seq(
      "edu.gemini.ocs" %% "edu-gemini-pit-launcher" % version.value,
      "org.osgi" % "org.osgi.core" % "4.2.0"
    ),
    appName := "Gemini PIT",
    maintainer in Universal := "Gemini Software Group",
    mappings in (Compile, packageDoc) := Seq(),
    mainClass in Compile := Some("edu.gemini.pit.launcher.PITLauncher"),
    id := "edu.gemini.pit",
    signatureID := "T87F4ZD75E",
    certificateHash := "60464455CE099B3293643BC0021D1F25D2F52A59",
    notarizationUID := "its@gemini.edu",
    icon := "PIT.icns",
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
      r -> ("Resources/" + r.getName)
    },
    javaOptions in Universal ++= Seq(
      "-no-version-check",
      "-java-home ${app_home}/../jre/Contents/Home"
    ),
    // Don't create launchers for Windows
    makeBatScripts := Seq.empty
  )
