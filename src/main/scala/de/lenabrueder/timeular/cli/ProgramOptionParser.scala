package de.lenabrueder.timeular.cli

import java.io.File
import java.time.LocalDateTime

import scopt.Read

object ProgramOptionParser extends ((Array[String], Map[String, String]) => Option[Config]) {
  implicit val localDateTimeRead: Read[LocalDateTime] = new Read[LocalDateTime] {
    override def arity: Int                     = 5
    override def reads: String => LocalDateTime = LocalDateTime.parse
  }
  override def apply(cliArgs: Array[String], envArgs: Map[String, String]): Option[Config] = {
    import scopt.OParser
    val builder = OParser.builder[CliConfig]
    val parser1 = {
      import builder._
      OParser.sequence(
        programName("timeular-cli"),
        head("timeular-cli", "1.0-SNAPSHOT"),
        opt[String]("apiKey")
          .action((x, c) => c.copy(apiKey = Some(x)))
          .text("The timeular API key."),
        opt[String]("apiSecret")
          .action((x, c) => c.copy(apiSecret = Some(x)))
          .text("The timeular API secret."),
        opt[String]("apiServer")
          .action((x, c) => c.copy(timeularServer = Some(x)))
          .text("The timeular API server."),
        opt[String]('f', "output-format")
          .action((x, c) => c.copy(outputType = Some(x)))
          .text("The output format to be used."),
        opt[File]('o', "output-file")
          .action((x, c) => c.copy(outputFile = Some(x)))
          .text("The file to export to. Skip for writing to stdout."),
        cmd("start")
          .action((x, c) => c.copy(command = Some("start")))
          .text("start tracking an activity"),
        cmd("stop")
          .action((x, c) => c.copy(command = Some("stop")))
          .text("stop tracking an activity"),
        cmd("export")
          .action((x, c) => c.copy(command = Some("export")))
          .text("stop tracking an activity")
          .children(
            opt[LocalDateTime]("startTime")
              .text("Start time of when the export starts."),
            opt[LocalDateTime]("endTime")
              .text("End time of when the export ends.")
          )
      )
    }

    OParser.parse(parser1, cliArgs, CliConfig()).map(Config.apply(_, envArgs))
  }
}
