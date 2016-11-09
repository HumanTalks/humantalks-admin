package com.humantalks.internal.venues

import com.humantalks.common.values.{ GMapPlace, Meta }
import com.humantalks.common.services.TwitterSrv
import global.values.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json
import com.humantalks.internal.persons.Person

case class Venue(
  id: Venue.Id,
  meetupId: Option[Long],
  data: Venue.Data,
  meta: Meta
)
object Venue {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Venue.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      name: String, // nom de la société / salle
      location: Option[GMapPlace],
      capacity: Option[Int], // nombre de place
      twitter: Option[String],
      logo: Option[String],
      contacts: List[Person.Id],
      comment: Option[String] // information supplémentaires
  ) {
    def trim: Data = copy(
      name = name.trim,
      twitter = twitter.map(TwitterSrv.toAccount),
      logo = logo.map(_.trim),
      comment = comment.map(_.trim)
    )
  }

  implicit val formatData = Json.format[Venue.Data]
  implicit val format = Json.format[Venue]
  val fields = mapping(
    "name" -> nonEmptyText,
    "location" -> optional(GMapPlace.fields),
    "capacity" -> optional(number),
    "twitter" -> optional(text),
    "logo" -> optional(text),
    "contacts" -> list(of[Person.Id]),
    "comment" -> optional(text)
  )(Venue.Data.apply)(Venue.Data.unapply)
}
