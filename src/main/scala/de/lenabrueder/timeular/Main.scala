package de.lenabrueder.timeular

import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.cli.Command
import de.lenabrueder.timeular.cli.Config
import de.lenabrueder.timeular.cli.ProgramOptionParser
import sttp.client.HttpURLConnectionBackend

import scala.jdk.CollectionConverters._

object Main extends App with StrictLogging {
  val programOptionParser: (Array[String], Map[String, String]) => Option[Config] = ProgramOptionParser
  val optConfig                                                                   = programOptionParser(args, System.getenv().asScala.toMap)

  implicit val backend = HttpURLConnectionBackend()

  optConfig match {
    case Some(config) =>
      implicit val cfg = config
      Command
        .run(config.command)
        .left
        .foreach { error =>
          logger.error(error)
          System.exit(2)
        }
    case None => System.exit(1)
  }
}
