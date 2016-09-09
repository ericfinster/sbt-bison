
val sbtBison = (project in file(".")).
  settings(
    name := "sbt-bison",
    organization := "net.opetopic",
    sbtPlugin := true,
    scalaVersion := "2.10.6",
    scalacOptions := Seq("-deprecation", "-unchecked", "-feature")
    // libraryDependencies ++= Seq(
    //   "edu.umass.cs.iesl" % "jflex-scala" % "1.6.1" % "compile"
    // )
  )
