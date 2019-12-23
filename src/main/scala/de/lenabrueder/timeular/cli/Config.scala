package de.lenabrueder.timeular.cli

import java.io.File
import java.time.LocalDateTime

import de.lenabrueder.timeular.cli.Config.OutputType

object Config {
  //TODO: this should work with some kind of plugin system, or maybe highly configurable
  type OutputType = String
}

sealed trait Command
case class Export(
    file: Option[File],
    startTime: LocalDateTime,
    endTime: LocalDateTime
) extends Command

case class Config(
    apiKey: String = "",
    apiSecret: String = "",
    output: OutputType = "text",
    timeularServer: String = "https://api.timeular.com/api/v2",
    command: Option[Command] = None
)
