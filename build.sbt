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
  // .enablePlugins(JavaAppPackaging)
    // .settings(NativePackagerKeys.packageArchetype.java_server:_*)
      .settings(
    libraryDependencies ++= Seq(
      "edu.gemini.ocs" %% "edu-gemini-pit-launcher" % "2020102.1.0",
      "org.osgi" % "org.osgi.core" % "4.2.0"
    ),
    name in Universal := "Gemini PIT",
    maintainer in Universal := "Gemini Software Group",
    mappings in (Compile, packageDoc) := Seq(),
    mainClass in Compile := Some("edu.gemini.pit.launcher.PITLauncher"),
    // universalArchiveOptions in (Universal, packageOsxDmg) := Seq("--verbose"),
     // packageBin in NotarizedFormat := {
     //        // val fileMappings = (mappings in Universal).value
     //        // // val output = target.value / s"${packageName.value}.txt"
     //        // val output = target.value / s"test.txt"
     //        // // create the is with the mappings. Note this is not the ISO format -.-
     //        // IO.write(output, "# Filemappings\n")
     //        // // append all mappings to the list
     //        // fileMappings foreach {
     //        //     case (file, name) => IO.append(output, s"${file.getAbsolutePath}\t$name${IO.Newline}")
     //        // }

     //        ???
     //    }
  )
