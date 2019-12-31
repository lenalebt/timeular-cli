package de.lenabrueder.timeular.api

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

import cats.Show
import cats.implicits._
import com.typesafe.scalalogging.StrictLogging
import io.circe.Decoder
import io.circe.Encoder
import io.circe.Json
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
  implicit val show                                     = Show.fromToString[SigninRequest]
}

case class AccessToken(
    token: String //send as "Authorization: Bearer $token"
)
object AccessToken {
  implicit val decoder: Decoder[AccessToken] = deriveDecoder
  implicit val encoder: Encoder[AccessToken] = deriveEncoder
  implicit val show: Show[AccessToken]       = Show.fromToString
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
  implicit val show: Show[Activity]       = Show.show(_.name)
}

case class Duration(
    startedAt: OffsetDateTime,
    stoppedAt: OffsetDateTime
) {
  lazy val asJavaDuration: java.time.Duration = java.time.Duration.between(startedAt, stoppedAt)
}
object Duration {
  import TimeularApiClient._
  implicit val decoder: Decoder[Duration] = deriveDecoder
  implicit val encoder: Encoder[Duration] = deriveEncoder
  val humanDateFormatter                  = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
  implicit val show: Show[Duration] = Show.show { x =>
    def format(date: OffsetDateTime) = humanDateFormatter.format(date)
    s"${format(x.startedAt)} until ${format(x.stoppedAt)}"
  }
}

case class Tag(
    indices: Seq[Int],
    key: String
)
object Tag {
  implicit val decoder: Decoder[Tag] = deriveDecoder
  implicit val encoder: Encoder[Tag] = deriveEncoder
  implicit val show: Show[Tag]       = Show.show(_.key)
}

case class Note(
    text: Option[String],
    tags: Seq[Tag]
)
object Note {
  implicit val decoder: Decoder[Note] = deriveDecoder
  implicit val encoder: Encoder[Note] = deriveEncoder
  implicit val show: Show[Note]       = Show.show(_.text.getOrElse(""))
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
  implicit val show: Show[TimeEntry] = Show.show { x =>
    s"${x.duration.show}: ${x.activity.show} ${if (x.note.text.isDefined) s"(${x.note.show})" else ""}"
  }
}

case class CurrentTrackingNote(
    text: String,
    tags: Seq[Tag],
    mentions: Seq[Tag]
)
object CurrentTrackingNote {
  implicit val decoder: Decoder[CurrentTrackingNote] = deriveDecoder
  implicit val encoder: Encoder[CurrentTrackingNote] = deriveEncoder
  implicit val show: Show[CurrentTrackingNote]       = Show.fromToString
}

case class CurrentTracking(
    activity: Activity,
    startedAt: OffsetDateTime,
    note: Option[CurrentTrackingNote]
)
object CurrentTracking {
  import TimeularApiClient._
  implicit val decoder: Decoder[CurrentTracking] = deriveDecoder
  implicit val encoder: Encoder[CurrentTracking] = deriveEncoder
  implicit val show: Show[CurrentTracking]       = Show.fromToString
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
  val dateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS").withZone(ZoneId.of("UTC"))
  implicit val offsetDateTimeEncoder: Encoder[OffsetDateTime] =
    Encoder.encodeOffsetDateTimeWithFormatter(dateTimeFormatter)
  implicit val offsetDateTimeDecoder: Decoder[OffsetDateTime] =
    Decoder.decodeLocalDateTimeWithFormatter(dateTimeFormatter).map(_.atOffset(ZoneOffset.UTC))

  def apply(baseUrl: String = baseUrl)(implicit backend: SttpBackend[Identity, Nothing, NothingT]): TimeularApiClient =
    new TimeularApiClient(baseUrl)
}
class LoggedInTimeularApiClient(
    val accessToken: AccessToken,
    val baseUrl: String
)(implicit private val backend: SttpBackend[Identity, Nothing, NothingT])
    extends StrictLogging {
  type ResponseType[T] = Identity[Either[String, T]]

  private def now: OffsetDateTime = OffsetDateTime.now()
  val baseRequest                 = basicRequest.auth.bearer(accessToken.token)
  import TimeularApiClient.dateTimeFormatter

  def timeEntries(
      from: OffsetDateTime,
      until: OffsetDateTime = OffsetDateTime.now()
  ): ResponseType[Seq[TimeEntry]] = {
    implicit val myDecoder = implicitly[Decoder[Seq[TimeEntry]]].at("timeEntries")
    val response = baseRequest
      .get(uri"$baseUrl/time-entries/${from.format(dateTimeFormatter)}/${until.format(dateTimeFormatter)}")
      .response(asJson[Seq[TimeEntry]])
      .send()
    response.body.leftMap(_ => "error").map(_.sortBy(_.duration.startedAt))
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

  def activities(): ResponseType[Seq[Activity]] = {
    implicit val myDecoder = implicitly[Decoder[Seq[Activity]]].at("activities")
    val response           = baseRequest.get(uri"$baseUrl/activities").response(asJson[Seq[Activity]]).send()
    response.body.leftMap(_ => "error")
  }

  def start(activity: Activity, note: Option[CurrentTrackingNote]): ResponseType[CurrentTracking] = {
    implicit val myDecoder = implicitly[Decoder[CurrentTracking]].at("currentTracking")
    val body               = CurrentTracking(activity, now, note)
    val response = baseRequest
      .post(uri"$baseUrl/tracking/${activity.id}/start")
      .response(asJson[CurrentTracking])
      .body(body.asJson)
      .send()
    response.body.leftMap(_ => "error")
  }
  def stop(activity: Activity): ResponseType[TimeEntry] = {
    implicit val myDecoder = implicitly[Decoder[TimeEntry]].at("createdTimeEntry")
    val response = baseRequest
      .post(uri"$baseUrl/tracking/${activity.id}/stop")
      .response(asJson[TimeEntry])
      .body(Json.fromFields(Seq("stoppedAt" -> Json.fromString(dateTimeFormatter.format(now)))))
      .send()
    response.body.leftMap(_ => "error")
  }
}

object LoggedInTimeularApiClient {
  def apply(accessToken: AccessToken, baseUrl: String = TimeularApiClient.baseUrl)(
      implicit backend: SttpBackend[Identity, Nothing, NothingT]): LoggedInTimeularApiClient =
    new LoggedInTimeularApiClient(accessToken, baseUrl)
}
