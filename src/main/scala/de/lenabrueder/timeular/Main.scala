package de.lenabrueder.timeular

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.typesafe.scalalogging.StrictLogging
import de.lenabrueder.timeular.cli.Command
import de.lenabrueder.timeular.cli.Config
import de.lenabrueder.timeular.cli.ProgramOptionParser
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._

object Main extends App with StrictLogging {
  val programOptionParser: (Array[String], Map[String, String]) => Option[Config] = ProgramOptionParser
  val optConfig                                                                   = programOptionParser(args, System.getenv().asScala.toMap)

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  implicit val system = ActorSystem()
  system.registerOnTermination {
    System.exit(0)
  }
  implicit val materializer = Materializer.matFromSystem

  implicit val wsCLient: StandaloneWSClient = StandaloneAhcWSClient()

  val result = optConfig match {
    case Some(config) =>
      implicit val cfg = config
      Command.run(config.command)
    case None => Future.failed(new IllegalStateException())
  }

  Await.result(result, 10.seconds)

  wsCLient.close()
  system.terminate()
}
