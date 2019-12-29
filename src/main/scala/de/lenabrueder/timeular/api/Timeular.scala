package de.lenabrueder.timeular.api

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.Encoder
import io.circe.generic.semiauto.deriveDecoder
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax._
import sttp.client._
import sttp.client.circe._

//see timeular API at https://developers.timeular.com/public-api/

case class SigninRequest(
    apiKey: String,
    apiSecret: String
)
object SigninRequest {
  implicit val decoder: io.circe.Decoder[SigninRequest] = deriveDecoder
  implicit val encoder: io.circe.Encoder[SigninRequest] = deriveEncoder
}

case class AccessToken(
    token: String //send as "Authorization: Bearer $token"
)
object AccessToken {
  implicit val decoder: Decoder[AccessToken] = deriveDecoder
  implicit val encoder: Encoder[AccessToken] = deriveEncoder
}

case class Activity(
    id: String,
    name: String,
    color: String,
    integration: String
)
object Activity {
  implicit val decoder: Decoder[Activity] = deriveDecoder
  implicit val encoder: Encoder[Activity] = deriveEncoder
}

case class Duration(
    startedAt: LocalDateTime,
    stoppedAt: LocalDateTime
) {
  lazy val asJavaDuration: java.time.Duration = java.time.Duration.between(startedAt, stoppedAt)
}
object Duration {
  implicit val decoder: Decoder[Duration] = deriveDecoder
  implicit val encoder: Encoder[Duration] = deriveEncoder
}

case class Tag(
    indices: Seq[Int],
    key: String
)
object Tag {
  implicit val decoder: Decoder[Tag] = deriveDecoder
  implicit val encoder: Encoder[Tag] = deriveEncoder
}

case class Note(
    text: Option[String],
    tags: Seq[Tag]
)
object Note {
  implicit val decoder: Decoder[Note] = deriveDecoder
  implicit val encoder: Encoder[Note] = deriveEncoder
}

case class TimeEntry(
    id: String,
    activity: Activity,
    duration: Duration,
    note: Note,
    mentions: Option[Seq[Tag]]
)
object TimeEntry {
  implicit val decoder: Decoder[TimeEntry] = deriveDecoder
  implicit val encoder: Encoder[TimeEntry] = deriveEncoder
}

case class CurrentTrackingNote(
    text: String,
    tags: Seq[Tag],
    mentions: Seq[Tag]
)
object CurrentTrackingNote {
  implicit val decoder: Decoder[CurrentTrackingNote] = deriveDecoder
  implicit val encoder: Encoder[CurrentTrackingNote] = deriveEncoder
}

case class CurrentTracking(
    activity: Activity,
    startedAt: LocalDateTime,
    note: CurrentTrackingNote
)
object CurrentTracking {
  implicit val decoder: Decoder[CurrentTracking] = deriveDecoder
  implicit val encoder: Encoder[CurrentTracking] = deriveEncoder
}

class TimeularApiClient(
    val baseUrl: String
)(implicit private val backend: SttpBackend[Identity, Nothing, NothingT])
    extends StrictLogging {
  type ResponseType[T] = Identity[Either[String, T]]

  def login(credentials: SigninRequest): ResponseType[LoggedInTimeularApiClient] = {

    val response = basicRequest
      .body(credentials.asJson)
      .post(uri"$baseUrl/developer/sign-in")
      .response(asJson[AccessToken])
      .send()
    response.body.bimap(_ => "error", LoggedInTimeularApiClient(_))
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
    implicit val myDecoder = implicitly[Decoder[Seq[TimeEntry]]].at("timeEntries")
    val response = baseRequest
      .get(uri"$baseUrl/time-entries/${from.format(dateTimeFormatter)}/${until.format(dateTimeFormatter)}")
      .response(asJson[Seq[TimeEntry]])
      .send()
    response.body.leftMap(_ => "error")
  }

  def timeEntry(id: String): ResponseType[TimeEntry] = {
    val response = baseRequest.get(uri"$baseUrl/time-entries/$id").response(asJson[TimeEntry]).send()
    response.body.leftMap(_ => "error")
  }

  def currentTracking(): ResponseType[Option[CurrentTracking]] = {
    implicit val myDecoder = implicitly[Decoder[Option[CurrentTracking]]].at("currentTracking")
    val response           = baseRequest.get(uri"$baseUrl/tracking").response(asJson[Option[CurrentTracking]]).send()
    response.body.leftMap(_ => "error")
  }
}

object LoggedInTimeularApiClient {
  def apply(accessToken: AccessToken, baseUrl: String = TimeularApiClient.baseUrl)(
      implicit backend: SttpBackend[Identity, Nothing, NothingT]): LoggedInTimeularApiClient =
    new LoggedInTimeularApiClient(accessToken, baseUrl)
}
