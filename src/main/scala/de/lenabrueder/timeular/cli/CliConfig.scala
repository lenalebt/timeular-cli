package de.lenabrueder.timeular.cli

import java.io.File
import java.time.DayOfWeek
import java.time.OffsetDateTime
import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.api.Duration
import de.lenabrueder.timeular.cli.CliConfig.OutputType

import java.util.UUID
import java.util.concurrent.TimeUnit
import scala.concurrent.duration

object CliConfig {
  //TODO: this should work with some kind of plugin system, or maybe highly configurable
  type OutputType = String
}

case class CliConfig(
    apiKey: Option[String] = None,
    apiSecret: Option[String] = None,
    outputType: Option[String] = None,
    outputFile: Option[File] = None,
    outputOptions: Option[Map[String, String]] = None,
    timeularServer: Option[String] = None,
    command: Option[String] = None,
    startTime: Option[OffsetDateTime] = None,
    endTime: Option[OffsetDateTime] = None,
    activity: Option[String] = None,
    holidayTagId: Option[UUID] = None,
    dailyWorkTarget: Option[Double] = None,
    workDays: Seq[DayOfWeek] =
      Seq(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
)
case class OutputOptions(
    file: Option[File],
    `type`: OutputType,
    options: Map[String, String]
)
case class Config(
    apiKey: String,
    apiSecret: String,
    timeularServer: String,
    command: Command,
    outputOptions: OutputOptions,
    dailyWorkTarget: duration.Duration,
    holidayTagId: Option[UUID],
    workDays: Seq[DayOfWeek]
)
object Config extends StrictLogging {
  val defaultTimeularServer = "https://api.timeular.com/api/v2"
  val defaultCommand        = "report"

  def apply(cliConfig: CliConfig, env: Map[String, String]): Config = {
    import cliConfig._
    Config(
      apiKey = apiKey.orElse(env.get("TIMEULAR_API_KEY")).get,
      apiSecret = apiSecret.orElse(env.get("TIMEULAR_API_SECRET")).get,
      timeularServer = timeularServer.orElse(env.get("TIMEULAR_SERVER")).getOrElse(defaultTimeularServer),
      command = Command(cliConfig),
      outputOptions = OutputOptions(
        file = outputFile,
        `type` = outputType.getOrElse("text"),
        options = outputOptions.getOrElse(Map.empty)
      ),
      dailyWorkTarget = duration.Duration.apply(cliConfig.dailyWorkTarget.getOrElse(8.0), TimeUnit.HOURS),
      holidayTagId = cliConfig.holidayTagId,
      workDays = cliConfig.workDays
    )
  }
}
