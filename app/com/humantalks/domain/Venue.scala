package com.humantalks.domain

import com.humantalks.domain.values.{ Meta, GMapPlace }
import global.models.{ TypedIdHelper, TypedId }
import play.api.data.Forms._
import play.api.libs.json.Json

case class VenueContact(
  name: String,
  email: Option[String],
  phone: Option[String],
  comment: Option[String]
)
object VenueContact {
  implicit val format = Json.format[VenueContact]
  val fields = mapping(
    "name" -> text.verifying("error.required", value => value.length > 0),
    "email" -> optional(email),
    "phone" -> optional(text),
    "comment" -> optional(text)
  )(VenueContact.apply)(VenueContact.unapply)
}

case class VenueData(
  name: String, // nom de la société / salle
  contact: Option[VenueContact],
  location: Option[GMapPlace],
  capacity: Option[Int], // nombre de place
  twitter: Option[String],
  logo: Option[String],
  comment: Option[String] // information supplémentaires
)
object VenueData {
  implicit val format = Json.format[VenueData]
  val fields = mapping(
    "name" -> nonEmptyText,
    "contact" -> optional(VenueContact.fields),
    "location" -> optional(GMapPlace.fields),
    "capacity" -> optional(number),
    "twitter" -> optional(text),
    "logo" -> optional(text),
    "comment" -> optional(text)
  )(VenueData.apply)(VenueData.unapply)
}

case class Venue(
  id: Venue.Id,
  data: VenueData,
  meta: Meta
)
object Venue {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Venue.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  implicit val format = Json.format[Venue]
}
