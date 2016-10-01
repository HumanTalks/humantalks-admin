package com.humantalks.meetups

import com.humantalks.common.models.values.Meta
import com.humantalks.talks.Talk
import com.humantalks.venues.Venue
import global.models.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Meetup(
  id: Meetup.Id,
  data: Meetup.Data,
  published: Boolean,
  meta: Meta
)
object Meetup {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Meetup.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
    title: String,
    date: DateTime,
    venue: Option[Venue.Id],
    talks: List[Talk.Id],
    description: Option[String],
    roti: Option[String]
  )

  implicit val formatData = Json.format[Data]
  implicit val format = Json.format[Meetup]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Venue.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text)
  )(Meetup.Data.apply)(Meetup.Data.unapply)
}
