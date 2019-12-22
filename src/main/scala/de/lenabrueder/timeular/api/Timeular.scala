package de.lenabrueder.timeular.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import play.api.Logger
import play.api.http.HeaderNames
import play.api.libs.json.JsonNaming.SnakeCase
import play.api.libs.json.Json
import play.api.libs.json.JsonConfiguration
import play.api.libs.ws.WSClient
import play.api.libs.ws.WSRequest

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import play.api.libs.ws.JsonBodyReadables._
import play.api.libs.ws.JsonBodyWritables._
import utils.StrictLogging

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
)
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
)(implicit ws: WSClient, executionContext: ExecutionContext)
    extends StrictLogging {
  def login(credentials: SigninRequest): Future[LoggedInTimeularApiClient] =
    for {
      response <- ws
                   .url(s"$baseUrl/developer/sign-in")
                   .post(Json.toJson(credentials))
    } yield {
      val token = response.json.as[AccessToken]
      LoggedInTimeularApiClient(token)
    }
}
object TimeularApiClient {
  val baseUrl = "https://api.timeular.com/api/v2"

  def apply(baseUrl: String = baseUrl)(implicit ws: WSClient, executionContext: ExecutionContext): TimeularApiClient =
    new TimeularApiClient(baseUrl)
}
class LoggedInTimeularApiClient(
    val accessToken: AccessToken,
    val baseUrl: String
)(implicit ws: WSClient, executionContext: ExecutionContext)
    extends StrictLogging {
  val dateTimeFormatter =
    DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS")
  implicit class WithAuth(val ws: WSRequest) {
    def withAuth() = ws.withHttpHeaders(HeaderNames.AUTHORIZATION -> s"Bearer ${accessToken.token}")
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
      (response.json \ "timeEntries").as[Seq[TimeEntry]]
    }
  }

  def timeEntry(id: String): Future[TimeEntry] =
    for {
      response <- ws.url(s"$baseUrl/time-entries/$id").withAuth().get()
    } yield {
      response.json.as[TimeEntry]
    }

  def currentTracking(): Future[Option[CurrentTracking]] =
    for {
      response <- ws.url(s"$baseUrl/tracking").withAuth().get()
    } yield {
      logger.warn(s"${response.body}")
      (response.json \ "currentTracking").asOpt[CurrentTracking]
    }
}

object LoggedInTimeularApiClient {
  def apply(accessToken: AccessToken, baseUrl: String = TimeularApiClient.baseUrl)(
      implicit ws: WSClient,
      executionContext: ExecutionContext): LoggedInTimeularApiClient =
    new LoggedInTimeularApiClient(accessToken, baseUrl)
}
