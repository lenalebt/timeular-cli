package de.lenabrueder.timeular.cli

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.time.LocalDateTime

import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.`export`.SAPGuiExcelExport
import de.lenabrueder.timeular.api.SigninRequest
import de.lenabrueder.timeular.api.TimeularApiClient
import de.lenabrueder.timeular.cli.Config.defaultCommand
import sttp.client.Identity
import sttp.client.NothingT
import sttp.client.SttpBackend

import scala.concurrent.Future

object Command extends StrictLogging {
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

  def run(command: Command)(implicit config: Config, backend: SttpBackend[Identity, Nothing, NothingT]): Unit = {
    lazy val apiClient =
      TimeularApiClient(config.timeularServer).login(SigninRequest(config.apiKey, config.apiSecret))
    command match {
      case Start() => Future.failed(???) //TODO
      case Stop()  => Future.failed(???) //TODO
      case Export(startTime, endTime) =>
        logger.info("exporting data...")
        val data = for {
          client <- apiClient
          data   <- client.timeEntries(startTime, endTime)
        } yield data
        val outputData = config.outputOptions.`type` match {
          //TODO: allow for some kind of plugin system? or just very nicely configurable output?
          case "xls" => data.map { SAPGuiExcelExport.create }
        }

        config.outputOptions.file match {
          case Some(file) =>
            logger.info(s"writing to file $file")
            val outputStream = new BufferedOutputStream(new FileOutputStream(file))
            outputData.foreach(outputStream.write)
            outputStream.close()
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
