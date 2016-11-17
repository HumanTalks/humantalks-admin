package com.humantalks.internal.meetups

import com.humantalks.common.services.DateSrv
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.{ DateTimeConstants, LocalTime, DateTime }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Meetup(
    id: Meetup.Id,
    meetupRef: Option[Meetup.MeetupRef],
    data: Meetup.Data,
    meta: Meta
) {
  lazy val meetupUrl: Option[String] = meetupRef.map(r => s"http://www.meetup.com/fr-FR/${r.group}/events/${r.id}/")
}
object Meetup {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Meetup.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long, announced: Boolean)

  case class Data(
      title: String,
      date: DateTime,
      venue: Option[Venue.Id],
      talks: List[Talk.Id],
      description: Option[String],
      roti: Option[String],
      personCount: Option[Int]
  ) {
    def trim: Data = copy(
      title = title.trim,
      description = description.map(_.trim),
      roti = roti.map(_.trim)
    )
  }
  object Data {
    def generate(after: DateTime): Data = {
      val nextDate = DateSrv.nextDate(after, 2, DateTimeConstants.TUESDAY, new LocalTime(19, 0))
      val nextTitle = Meetup.title(nextDate)
      Meetup.Data(nextTitle, nextDate, None, List(), None, None, None)
    }
  }

  def title(date: DateTime): String = "HumanTalks Paris " + date.toString("MMMM YYYY")
  def slackChannel(date: DateTime): String = date.toString("YYYY_MM")

  implicit val formatData = Json.format[Data]
  implicit val formatRef = Json.format[MeetupRef]
  implicit val format = Json.format[Meetup]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Venue.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text),
    "personCount" -> optional(number)
  )(Meetup.Data.apply)(Meetup.Data.unapply)
}
