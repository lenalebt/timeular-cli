package de.lenabrueder.timeular.cli

import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileOutputStream
import java.time.LocalDateTime

import de.lenabrueder.timeular.`export`.SAPGuiExcelExport
import de.lenabrueder.timeular.api.SigninRequest
import de.lenabrueder.timeular.api.TimeularApiClient
import de.lenabrueder.timeular.cli.Config.defaultCommand
import play.api.libs.ws.StandaloneWSClient

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

object Command {
  def apply(cliOptions: CliConfig): Command = {
    import cliOptions._
    command.getOrElse(defaultCommand).toLowerCase match {
      case "export" =>
        Export(startTime = startTime.getOrElse(LocalDateTime.now().minusDays(1)),
               endTime = endTime.getOrElse(LocalDateTime.now()))
      case "start" => Start()
      case "stop"  => Stop()
    }
  }

  def run(command: Command)(implicit config: Config, ws: StandaloneWSClient, ec: ExecutionContext): Future[Unit] = {
    lazy val apiClient =
      new TimeularApiClient(config.timeularServer).login(SigninRequest(config.apiKey, config.apiSecret))
    command match {
      case Start() => Future.failed(???)
      case Stop()  => Future.failed(???)
      case Export(startTime, endTime) =>
        val data = for {
          client <- apiClient
          data   <- client.timeEntries(startTime, endTime)
        } yield data
        val outputData = config.outputOptions.`type` match {
          case "xls" => data.map { SAPGuiExcelExport.create }
        }

        config.outputOptions.file match {
          case Some(file) =>
            val outputStream = new BufferedOutputStream(new FileOutputStream(file))
            outputData.map(outputStream.write).map(_ => outputStream.close())
          case None => outputData.map(println)
        }
    }
  }
}

sealed trait Command
case class Export(
    startTime: LocalDateTime,
    endTime: LocalDateTime
) extends Command
case class Start() extends Command
case class Stop()  extends Command
