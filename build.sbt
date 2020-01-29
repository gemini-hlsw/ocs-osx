import NativePackagerHelper._

name in ThisBuild := "ocs-osx"

Global / onChangedBuildSource := ReloadOnSourceChanges

scalaVersion in ThisBuild := "2.11.11"

organization := "edu.gemini"

description := "MacOS builder of legacy OT apps"

homepage := Some(url("https://github.com/gemini-hlsw/ocs-osx"))

licenses := Seq(
  "BSD 3-Clause License" -> url(
    "https://opensource.org/licenses/BSD-3-Clause"))

addCommandAlias(
  "pitDmg",
  "; pit/clean; pit/universal:packageOsxDmg")

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
    name in Universal := "Gemini PIT",
    maintainer in Universal := "Gemini Software Group",
    mappings in (Compile, packageDoc) := Seq(),
    mainClass in Compile := Some("edu.gemini.pit.launcher.PITLauncher"),
    id := "edu.gemini.pit",
    mappings in Universal ++= {
      val jresDir = Path.userHome / ".jres13"
      val osxJre = jresDir.toPath.resolve("jre")
      directory(osxJre.toFile).map { j =>
        j._1 -> j._2
      }
    },
    javaOptions in Universal ++= Seq(
      "-java-home ${app_home}/../jre/Contents/Home"
    )
  )
