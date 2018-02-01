package com.humantalks.internal.events

import java.util.Locale

import com.humantalks.common.services.DateSrv
import com.humantalks.common.values.{ GMapPlace, Meta }
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.Partner
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.{ DateTime, DateTimeConstants, LocalTime }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Event(
    id: Event.Id,
    meetupRef: Option[Event.MeetupRef],
    data: Event.Data,
    meta: Meta
) {
  lazy val meetupUrl: Option[String] = meetupRef.map(r => s"http://www.meetup.com/fr-FR/${r.group}/events/${r.id}/")
  lazy val slackChannel: String = Event.slackChannel(data.date)
  lazy val allPartners: List[Partner.Id] = data.venue.toList ++ data.apero.toList
}
object Event {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Event.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long)

  case class Data(
      title: String,
      date: DateTime,
      venue: Option[Partner.Id],
      location: Option[GMapPlace],
      apero: Option[Partner.Id],
      talks: List[Talk.Id],
      description: Option[String],
      roti: Option[String],
      personCount: Option[Int]
  ) {
    lazy val slackChannel: String = Event.slackChannel(date)
    def trim: Data = copy(
      title = title.trim,
      description = description.map(_.trim),
      roti = roti.map(_.trim)
    )
  }
  object Data {
    def generate(after: DateTime): Data = {
      val nextDate = DateSrv.nextDate(after, 2, DateTimeConstants.TUESDAY, new LocalTime(19, 0))
      val nextTitle = Event.title(nextDate)
      Event.Data(nextTitle, nextDate, None, None, None, List(), None, None, None)
    }
  }

  def title(date: DateTime): String = "HumanTalks Paris " + date.toString("MMMM YYYY", Locale.FRANCE).capitalize
  def slackChannel(date: DateTime): String = date.toString("YYYY_MM")

  implicit val formatData = Json.format[Data]
  implicit val formatRef = Json.format[MeetupRef]
  implicit val format = Json.format[Event]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Partner.Id]),
    "location" -> ignored(Option.empty[GMapPlace]),
    "apero" -> optional(of[Partner.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text),
    "personCount" -> optional(number)
  )(Event.Data.apply)(Event.Data.unapply)
}
