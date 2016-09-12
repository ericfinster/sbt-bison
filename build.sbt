
val sbtBison = (project in file(".")).
  settings(
    name := "sbt-bison",
    organization := "net.opetopic",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    scalacOptions := Seq("-deprecation", "-unchecked", "-feature")
    // libraryDependencies ++= Seq(
    //   "edu.uwm.cs" %% "scala-bison" % "0.1-SNAPSHOT"
    // )
  )
