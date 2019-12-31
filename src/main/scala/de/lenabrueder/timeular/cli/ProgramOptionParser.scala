package de.lenabrueder.timeular.cli

import java.io.File
import java.time.LocalDate
import java.time.ZoneId
import java.time.OffsetDateTime

import cats._
import cats.implicits._
import scopt.Read

import scala.util.Try

object ProgramOptionParser extends ((Array[String], Map[String, String]) => Option[Config]) {
  implicit def localDateTimeRead(useEndOfDay: Boolean): Read[OffsetDateTime] = new Read[OffsetDateTime] {
    override def arity: Int = 5
    override def reads: String => OffsetDateTime =
      x =>
        Try(OffsetDateTime.parse(x)).orElse {
          lazy val date = LocalDate.parse(x).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime
          Try(if (useEndOfDay) { date.plusDays(1).minusSeconds(1) } else { date })
        }.get
  }
  override def apply(cliArgs: Array[String], envArgs: Map[String, String]): Option[Config] = {
    import scopt.OParser
    val builder = OParser.builder[CliConfig]
    val parser1 = {
      import builder._
      OParser.sequence(
        programName("timeular-cli"),
        head("timeular-cli", "1.0-SNAPSHOT"),
        opt[String]("api-key")
          .action((x, c) => c.copy(apiKey = Some(x)))
          .text("The timeular API key."),
        opt[String]("api-secret")
          .action((x, c) => c.copy(apiSecret = Some(x)))
          .text("The timeular API secret."),
        opt[String]("api-server")
          .action((x, c) => c.copy(timeularServer = Some(x)))
          .text("The timeular API server."),
        opt[String]('o', "output-format")
          .action((x, c) => c.copy(outputType = Some(x)))
          .text("The output format to be used."),
        opt[File]('f', "output-file")
          .action((x, c) => c.copy(outputFile = Some(x)))
          .text("The file to export to. Skip for writing to stdout. " +
            "Writing to stdout for binary formats (such as xls) is currently not supported."),
        opt[Map[String, String]]('k', "output-options")
          .action((x, c) => c.copy(outputOptions = Some(x)))
          .text("The output options for the format. Depends on the specific output format. Try e.g. 'report=true'"),
        cmd("start")
          .action((x, c) => c.copy(command = Some("start")))
          .text("start tracking an activity")
          .children(arg[String]("activity").action((x, c) => c.copy(activity = Some(x)))),
        cmd("stop")
          .action((x, c) => c.copy(command = Some("stop")))
          .text("stop tracking an activity")
          .children(arg[String]("activity").action((x, c) => c.copy(activity = Some(x))).optional()),
        cmd("list-activities")
          .action((x, c) => c.copy(command = Some("list-activities")))
          .text("list the activities known to timeular"),
        cmd("export")
          .action((x, c) => c.copy(command = Some("export")))
          .text("stop tracking an activity")
          .children(
            opt[OffsetDateTime]("start-time")(localDateTimeRead(false))
              .action((x, c) => c.copy(startTime = Some(x)))
              .text("Start time of when the export starts."),
            opt[OffsetDateTime]("end-time")(localDateTimeRead(true))
              .action((x, c) => c.copy(endTime = Some(x)))
              .text("End time of when the export ends."),
            checkConfig { cfg =>
              val startTimeBeforeEndTime = (for {
                startTime <- cfg.startTime
                endTime   <- cfg.endTime
              } yield startTime.isBefore(endTime)).getOrElse(true)
              if (startTimeBeforeEndTime) Right(()) else Left("'start-time' must be before 'end-time'!")
            }
          )
      )
    }

    OParser.parse(parser1, cliArgs, CliConfig()).map(Config.apply(_, envArgs))
  }
}
