package de.lenabrueder.timeular.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.Json
import sttp.client._

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
)(implicit private val backend: SttpBackend[Identity, Nothing, NothingT])
    extends StrictLogging {
  type ResponseType[T] = Identity[Either[String, T]]

  def login(credentials: SigninRequest): ResponseType[LoggedInTimeularApiClient] = {

    val response = basicRequest
      .body(Json.toJson(credentials).toString())
      .post(uri"$baseUrl/developer/sign-in")
      .send()
    response.body.map { it =>
      val token = Json.parse(it).as[AccessToken]
      LoggedInTimeularApiClient(token)
    }
  }
}
object TimeularApiClient {
  val baseUrl = "https://api.timeular.com/api/v2"

  def apply(baseUrl: String = baseUrl)(implicit backend: SttpBackend[Identity, Nothing, NothingT]): TimeularApiClient =
    new TimeularApiClient(baseUrl)
}
class LoggedInTimeularApiClient(
    val accessToken: AccessToken,
    val baseUrl: String
)(implicit private val backend: SttpBackend[Identity, Nothing, NothingT])
    extends StrictLogging {
  type ResponseType[T] = Identity[Either[String, T]]

  val dateTimeFormatter =
    DateTimeFormatter.ofPattern("YYYY-MM-dd'T'HH:mm:ss.SSS")
  val baseRequest = basicRequest.auth.bearer(accessToken.token)

  def timeEntries(
      from: LocalDateTime,
      until: LocalDateTime = LocalDateTime.now()
  ): ResponseType[Seq[TimeEntry]] = {
    val response = baseRequest
      .get(uri"$baseUrl/time-entries/${from.format(dateTimeFormatter)}/${until.format(dateTimeFormatter)}")
      .send()
    response.body.map { it =>
      (Json.parse(it) \ "timeEntries").as[Seq[TimeEntry]]
    }
  }

  def timeEntry(id: String): ResponseType[TimeEntry] = {
    val response = baseRequest.get(uri"$baseUrl/time-entries/$id").send()
    response.body.map { it =>
      Json.parse(it).as[TimeEntry]
    }
  }

  def currentTracking(): ResponseType[Option[CurrentTracking]] = {
    val response = baseRequest.get(uri"$baseUrl/tracking").send()
    response.body.map { it =>
      (Json.parse(it) \ "currentTracking").asOpt[CurrentTracking]
    }
  }
}

object LoggedInTimeularApiClient {
  def apply(accessToken: AccessToken, baseUrl: String = TimeularApiClient.baseUrl)(
      implicit backend: SttpBackend[Identity, Nothing, NothingT]): LoggedInTimeularApiClient =
    new LoggedInTimeularApiClient(accessToken, baseUrl)
}
