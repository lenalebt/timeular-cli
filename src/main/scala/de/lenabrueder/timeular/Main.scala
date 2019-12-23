package de.lenabrueder.timeular

import cli.CliOptionParser
import cli.Config
import cli.Export
import com.typesafe.scalalogging.StrictLogging

import scala.jdk.CollectionConverters._

object Main extends App with StrictLogging {
  val cliOptionParser: (Array[String], Map[String, String]) => Option[Config] = CliOptionParser

  val optConfig = cliOptionParser(args, System.getenv().asScala.toMap)

  optConfig match {
    case Some(config) =>
      config.command match {
        case Some(Export(file, startTime, endTime)) => logger.info(s"will do an export to file $file")
        case _                                      =>
      }
    case _ =>
  }
}
