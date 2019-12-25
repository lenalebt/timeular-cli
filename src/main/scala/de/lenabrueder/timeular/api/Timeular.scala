package de.lenabrueder.timeular.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import play.api.libs.ws.StandaloneWSClient
import play.api.libs.ws.StandaloneWSRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

//see timeular API at https://developers.timeular.com/public-api/

case class SigninRequest(
    apiKey: String,
    apiSecret: String
)
object SigninRequest {
  implicit val format = Json.format[SigninRequest]
}

case class AccessToken(
    token: String //send as "Authorization: Bearer $token"
)
object AccessToken {
  implicit val format = Json.format[AccessToken]
}

case class Activity(
    id: String,
    name: String,
    color: String,
    integration: String
)
object Activity {
  implicit val format = Json.format[Activity]
}

case class Duration(
    startedAt: LocalDateTime,
    stoppedAt: LocalDateTime
) {
  lazy val asJavaDuration: java.time.Duration = java.time.Duration.between(startedAt, stoppedAt)
}
object Duration {
  implicit val format = Json.format[Duration]
}

case class Tag(
    indices: Seq[Int],
    key: String
)
object Tag {
  implicit val format = Json.format[Tag]
}

case class Note(
    text: Option[String],
    tags: Seq[Tag]
)
object Note {
  implicit val format = Json.format[Note]
}

case class TimeEntry(
    id: String,
    activity: Activity,
    duration: Duration,
    note: Note,
    mentions: Option[Seq[Tag]]
)
object TimeEntry {
  implicit val format = Json.format[TimeEntry]
}

case class CurrentTrackingNote(
    text: String,
    tags: Seq[Tag],
    mentions: Seq[Tag]
)
object CurrentTrackingNote {
  implicit val format = Json.format[CurrentTrackingNote]
}

case class CurrentTracking(
    activity: Activity,
    startedAt: LocalDateTime,
    note: CurrentTrackingNote
)
object CurrentTracking {
  implicit val format = Json.format[CurrentTracking]
}

class TimeularApiClient(
    val baseUrl: String
)(implicit ws: StandaloneWSClient, executionContext: ExecutionContext)
    extends StrictLogging {
  def login(credentials: SigninRequest): Future[LoggedInTimeularApiClient] =
    for {
      response <- ws
                   .url(s"$baseUrl/developer/sign-in")
                   .post(Json.toJson(credentials))
    } yield {
      val token = response.body[JsValue].as[AccessToken]
      LoggedInTimeularApiClient(token)
    }
}
object TimeularApiClient {
  val baseUrl = "https://api.timeular.com/api/v2"

  def apply(baseUrl: String = baseUrl)(implicit ws: StandaloneWSClient,
                                       executionContext: ExecutionContext): TimeularApiClient =
    new TimeularApiClient(baseUrl)
}
class LoggedInTimeularApiClient(
    val accessToken: AccessToken,
    val baseUrl: String
)(implicit ws: StandaloneWSClient, executionContext: ExecutionContext)
    extends StrictLogging {
  val dateTimeFormatter =
    DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS")
  implicit class WithAuth(val ws: StandaloneWSRequest) {
    def withAuth() = ws.withHttpHeaders("Authorization" -> s"Bearer ${accessToken.token}")
  }

  def timeEntries(
      from: LocalDateTime,
      until: LocalDateTime = LocalDateTime.now()
  ): Future[Seq[TimeEntry]] = {
    for {
      response <- ws
                   .url(s"$baseUrl/time-entries/${from.format(dateTimeFormatter)}/${until.format(dateTimeFormatter)}")
                   .withAuth()
                   .get()
    } yield {
      logger.warn(s"response: ${response.body}")
      (response.body[JsValue] \ "timeEntries").as[Seq[TimeEntry]]
    }
  }

  def timeEntry(id: String): Future[TimeEntry] =
    for {
      response <- ws.url(s"$baseUrl/time-entries/$id").withAuth().get()
    } yield {
      response.body[JsValue].as[TimeEntry]
    }

  def currentTracking(): Future[Option[CurrentTracking]] =
    for {
      response <- ws.url(s"$baseUrl/tracking").withAuth().get()
    } yield {
      logger.warn(s"${response.body}")
      (response.body[JsValue] \ "currentTracking").asOpt[CurrentTracking]
    }
}

object LoggedInTimeularApiClient {
  def apply(accessToken: AccessToken, baseUrl: String = TimeularApiClient.baseUrl)(
      implicit ws: StandaloneWSClient,
      executionContext: ExecutionContext): LoggedInTimeularApiClient =
    new LoggedInTimeularApiClient(accessToken, baseUrl)
}
