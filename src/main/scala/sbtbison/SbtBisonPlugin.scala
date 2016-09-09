/*
 * Copyright 2016 Eric Finster
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sbtbnfc

import sbt._
import Keys._
import plugins._

object SbtBisonPlugin extends AutoPlugin {

  object autoImport {

    val bisonCommand = settingKey[String]("The bison command string")
    val bisonSrcDirectory = settingKey[File]("Directory for bison sources")
    val bisonSources = taskKey[Seq[File]]("The list of bison sources to process")

    val scalaBison = taskKey[Seq[File]]("Run the scala-bison command")
    val scalaBisonJar = settingKey[File]("The scala-bison.jar file")

  }

  import autoImport._

  override def requires = JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(

    bisonCommand := "bison",
    bisonSrcDirectory := (sourceDirectory in Compile).value / "bison",
    bisonSources := { (bisonSrcDirectory.value ** "*.y").get },

    scalaBisonJar := baseDirectory.value / "project" / "lib" / "scala-bison-2.11.jar",

    sourceGenerators in Compile += scalaBison.taskValue,

    scalaBison := {

      val bCmd = bisonCommand.value
      val sbjar = scalaBisonJar.value
      val inDir = bisonSrcDirectory.value
      val outDir = (sourceManaged in Compile).value
      val log = streams.value.log
      val sources = bisonSources.value

      val generatedFiles = 
        for {
          file <- sources
        } yield {

          // Copy the file to the target directory for processing
          val tgtFile = outDir / file.name

          log.info("Target file is: " + tgtFile.absolutePath)

          outDir.mkdirs()
            (file #> tgtFile).!

          log.info("Running bison on file: " + file.name)
          Process(bCmd :: "-v" :: tgtFile.name :: Nil, outDir) ! log

          log.info("Running scala-bison ...")

          val forkOpts = ForkOptions(
            bootJars = sbjar +: scalaInstance.value.allJars().toSeq,
            workingDirectory = Some(outDir)
          )

          val forkArgs = Seq("-howtorun:object", "edu.uwm.cs.scalabison.RunGenerator", "-v", tgtFile.name)

          Fork.scala(forkOpts, forkArgs)

          log.info("Generated sources for: " + tgtFile.base)

          Seq(
            outDir / (tgtFile.base + "Parser.scala"),
            outDir / (tgtFile.base + "Tokens.scala")
          )
        }

      generatedFiles.flatten

    }

  )

}
