package de.lenabrueder.timeular.cli

import java.io.File
import java.time.LocalDateTime

import scopt.Read

object CliOptionParser extends ((Array[String], Map[String, String]) => Option[Config]) {
  implicit val localDateTimeRead: Read[LocalDateTime] = new Read[LocalDateTime] {
    override def arity: Int                     = 5
    override def reads: String => LocalDateTime = LocalDateTime.parse
  }
  override def apply(cliArgs: Array[String], envArgs: Map[String, String]): Option[Config] = {
    import scopt.OParser
    val builder = OParser.builder[Config]
    val parser1 = {
      import builder._
      OParser.sequence(
        programName("timeular-cli"),
        head("timeular-cli", "1.0-SNAPSHOT"),
        opt[String]("apiKey")
          .required()
          .action((x, c) => c.copy(apiKey = x))
          .text("The timeular API key."),
        opt[String]("apiSecret")
          .required()
          .action((x, c) => c.copy(apiSecret = x))
          .text("The timeular API secret."),
        opt[String]("apiServer")
          .action((x, c) => c.copy(timeularServer = x))
          .text("The timeular API server."),
        opt[String]('o', "output")
          .action((x, c) => c.copy(timeularServer = x))
          .text("The output format to be used."),
        cmd("start")
          .text("start tracking an activity"),
        cmd("stop")
          .text("stop tracking an activity"),
        cmd("export")
          .text("stop tracking an activity")
          .children(
            opt[File]('f', "file")
              .text("The file to export to. Skip for writing to stdout."),
            opt[LocalDateTime]("startTime")
              .text("Start time of when the export starts."),
            opt[LocalDateTime]("endTime")
              .text("End time of when the export ends.")
          )
      )
    }

    OParser.parse(parser1, cliArgs, Config())
  }
}
