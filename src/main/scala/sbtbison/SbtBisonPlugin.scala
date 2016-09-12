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
    val bisonTgtDirectory = settingKey[File]("Directory for bison output")

    val scalaBison = taskKey[Seq[File]]("Run the scala-bison command")
    val scalaBisonJar = settingKey[File]("The scala-bison jar file")

  }

  import autoImport._

  override def requires = JvmPlugin
  override def trigger = allRequirements

  override lazy val projectSettings = Seq(

    bisonCommand := "bison",
    bisonSrcDirectory := (sourceDirectory in Compile).value / "bison",
    bisonTgtDirectory := (sourceManaged in Compile).value,

    scalaBisonJar := baseDirectory.value / "project" / "lib" / "scala-bison.jar",

    scalaBison := {

      val bCmd = bisonCommand.value
      val srcDir = bisonSrcDirectory.value
      val tgtDir = bisonTgtDirectory.value
      val scalaJars = scalaInstance.value.allJars().toSeq
      val sbJar = scalaBisonJar.value
      val log = streams.value.log

      val cc = FileFunction.cached(
        streams.value.cacheDirectory / "bison",
        inStyle = FilesInfo.lastModified,
        outStyle = FilesInfo.exists
      ) { (in : Set[File]) =>
        BisonGenerator.generateBison(
          srcDir, tgtDir, bCmd, sbJar, scalaJars, log
        ).toSet
      }

      cc((srcDir ** "*.y").get.toSet).toSeq

    },

    sourceGenerators in Compile += scalaBison.taskValue

  )

  object BisonGenerator {

    def generateBison(
      srcDir: File, tgtDir: File,
      bCmd: String, sbJar: File,
      scalaJars: Seq[File], log: Logger
    ): Seq[File] = {

      val sources = (srcDir ** ("*.y")).get

      log.info("Scala jars: " + scalaJars.toString)

      val fileLists =
        for {
          file <- sources
        } yield {

          // Copy the file to the target directory for processing
          val tgtFile = tgtDir / file.name

          log.info("Target file is: " + tgtFile.absolutePath)

          tgtDir.mkdirs()
          (file #> tgtFile).!

          log.info("Running bison on file: " + file.name)
          Process(bCmd :: "-v" :: tgtFile.name :: Nil, tgtDir) ! log

          log.info("Running scala-bison on generated parser ...")

          val forkOpts = ForkOptions(
            bootJars = sbJar +: scalaJars,
            workingDirectory = Some(tgtDir)
          )

          val forkArgs = Seq("-howtorun:object", "edu.uwm.cs.scalabison.RunGenerator", "-v", tgtFile.name)

          Fork.scala(forkOpts, forkArgs)

          Seq(
            tgtDir / (tgtFile.base + "Parser.scala"),
            tgtDir / (tgtFile.base + "Tokens.scala")
          )

        }

      fileLists.flatten

    }

  }

}
