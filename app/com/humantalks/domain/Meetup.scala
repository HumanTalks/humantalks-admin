package com.humantalks.domain

import com.humantalks.domain.values.Meta
import global.models.{ TypedIdHelper, TypedId }
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class MeetupData(
  title: String,
  date: DateTime,
  venue: Option[Venue.Id],
  talks: List[Talk.Id],
  description: Option[String],
  roti: Option[String]
)
object MeetupData {
  implicit val format = Json.format[MeetupData]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Venue.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text)
  )(MeetupData.apply)(MeetupData.unapply)
}

case class Meetup(
  id: Meetup.Id,
  data: MeetupData,
  published: Boolean,
  meta: Meta
)
object Meetup {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Meetup.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  implicit val format = Json.format[Meetup]
}
