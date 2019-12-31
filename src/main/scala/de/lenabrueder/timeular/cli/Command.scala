package de.lenabrueder.timeular.cli

import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.PrintWriter
import java.time.OffsetDateTime

import cats._
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.api.SigninRequest
import de.lenabrueder.timeular.api.TimeularApiClient
import de.lenabrueder.timeular.cli.Config.defaultCommand
import de.lenabrueder.timeular.output.OutputFormat
import de.lenabrueder.timeular.util.LongestCommonSeq
import io.circe.Encoder
import sttp.client.Identity
import sttp.client.NothingT
import sttp.client.SttpBackend

object Command extends StrictLogging {
  def apply(cliOptions: CliConfig): Command = {
    import cliOptions._
    command.getOrElse(defaultCommand).toLowerCase match {
      case "export" =>
        Export(startTime = startTime.getOrElse(OffsetDateTime.now().minusDays(1)),
               endTime = endTime.getOrElse(OffsetDateTime.now()))
      case "start"           => Start(cliOptions.activity.get)
      case "stop"            => Stop(cliOptions.activity)
      case "list-activities" => ListActivities()
    }
  }

  def run(command: Command)(implicit config: Config,
                            backend: SttpBackend[Identity, Nothing, NothingT]): Either[String, Unit] = {
    implicit def seqShow[T2: Show]: Show[Seq[T2]] = Show.show(_.map(_.show).mkString("\n"))
    def performOutput[T](data: T)(implicit encoder: Encoder[T], show: Show[T]): Unit = {

      //left is for binary formats, right is for text formats
      val outputData = OutputFormat.apply(data)

      config.outputOptions.file match {
        case Some(file) =>
          outputData match {
            case Right(textData) =>
              val outputStream = new PrintWriter(file)
              outputStream.println(textData)
              outputStream.close()
            case Left(binaryData) =>
              val outputStream = new BufferedOutputStream(new FileOutputStream(file))
              outputStream.write(binaryData)
              outputStream.close()
          }

        case None =>
          outputData match {
            case Right(textData)  => println(textData)
            case Left(binaryData) => println("Will not write binary data to console!")
          }
      }
    }
    def chooseActivity(activity: String) = {
      val activities = for {
        client <- apiClient
        data   <- client.activities()
      } yield data
      val chosenActivity = activities.map { act =>
        val map = act.map(it => it.name -> it).toMap
        val distanceMap = map.view.mapValues { it =>
          LongestCommonSeq.distance(activity.toLowerCase, it.name.toLowerCase)
        }.toMap
        val smallestDistanceActivity =
          map(distanceMap.maxBy(_._2)._1)
        smallestDistanceActivity
      }
      chosenActivity.foreach(it => logger.info(s"Chose activity ${it.name}"))
      chosenActivity
    }

    lazy val apiClient =
      TimeularApiClient(config.timeularServer).login(SigninRequest(config.apiKey, config.apiSecret))
    command match {
      case Start(activity) =>
        val chosenActivity = chooseActivity(activity)
        val result = for {
          activity <- chosenActivity
          client   <- apiClient
          data     <- client.start(activity, None)
        } yield data
        result.map(performOutput(_))

      case Stop(activity) =>
        lazy val currentTrackingActivity = (for {
          client <- apiClient
          data   <- client.currentTracking()
        } yield { data.map(_.activity) })

        val chosenActivity = activity
          .map(chooseActivity)
          .getOrElse(currentTrackingActivity.flatMap(_.toRight("no current activity to stop")))
        val result = for {
          activity <- chosenActivity
          client   <- apiClient
          data     <- client.stop(activity)
        } yield data
        result.map(performOutput(_))

      case ListActivities() =>
        val data = for {
          client <- apiClient
          data   <- client.activities()
        } yield data

        data.map(performOutput(_))
      case Export(startTime, endTime) =>
        logger.info("exporting data...")
        val data = for {
          client <- apiClient
          data   <- client.timeEntries(startTime, endTime)
        } yield data
        data.map(performOutput(_))
    }
  }
}

sealed trait Command
case class Export(
    startTime: OffsetDateTime,
    endTime: OffsetDateTime
) extends Command
case class ListActivities()               extends Command
case class Start(activity: String)        extends Command
case class Stop(activity: Option[String]) extends Command
