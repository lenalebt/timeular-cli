package de.lenabrueder.timeular.cli

import java.io.File
import java.time.LocalDateTime

import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.cli.CliConfig.OutputType

object CliConfig {
  //TODO: this should work with some kind of plugin system, or maybe highly configurable
  type OutputType = String
}

sealed trait Command
case class Export(
    startTime: LocalDateTime,
    endTime: LocalDateTime
) extends Command
case class Start() extends Command
case class Stop()  extends Command

case class CliConfig(
    apiKey: Option[String] = None,
    apiSecret: Option[String] = None,
    outputType: Option[String] = None,
    outputFile: Option[File] = None,
    timeularServer: Option[String] = None,
    command: Option[String] = None,
    startTime: Option[LocalDateTime] = None,
    endTime: Option[LocalDateTime] = None
)
case class OutputOptions(
    file: Option[File],
    `type`: OutputType
)
case class Config(
    apiKey: String,
    apiSecret: String,
    timeularServer: String,
    command: Command,
    outputOptions: OutputOptions
)
object Config extends StrictLogging {
  val defaultTimeularServer = "https://api.timeular.com/api/v2"
  val defaultCommand        = "report"

  private def parseCommand(cliOptions: CliConfig): Command = {
    import cliOptions._
    command.getOrElse(defaultCommand).toLowerCase match {
      case "export" =>
        Export(startTime = startTime.getOrElse(LocalDateTime.now().minusDays(1)),
               endTime = endTime.getOrElse(LocalDateTime.now()))
      case "start" => Start()
      case "stop"  => Stop()
    }
  }

  def apply(cliConfig: CliConfig, env: Map[String, String]): Config = {
    import cliConfig._
    logger.info(s"parsed cli options: $cliConfig")
    Config(
      apiKey = apiKey.orElse(env.get("TIMEULAR_API_KEY")).get,
      apiSecret = apiSecret.orElse(env.get("TIMEULAR_API_SECRET")).get,
      timeularServer = timeularServer.orElse(env.get("TIMEULAR_SERVER")).getOrElse(defaultTimeularServer),
      command = parseCommand(cliConfig),
      outputOptions = OutputOptions(
        file = outputFile,
        `type` = outputType.getOrElse("xls")
      )
    )
  }
}
