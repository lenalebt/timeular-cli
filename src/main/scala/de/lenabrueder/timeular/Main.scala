package de.lenabrueder.timeular

import cli.CliConfig
import cli.Config
import cli.Export
import cli.ProgramOptionParser
import com.typesafe.scalalogging.StrictLogging

import scala.jdk.CollectionConverters._

object Main extends App with StrictLogging {
  val programOptionParser: (Array[String], Map[String, String]) => Option[Config] = ProgramOptionParser

  val optConfig = programOptionParser(args, System.getenv().asScala.toMap)

  optConfig match {
    case Some(config) =>
      config.command match {
        case Export(startTime, endTime) => logger.info(s"will do an export to file ${config.outputOptions.file}")
        case _                          =>
      }
    case _ =>
  }
}
