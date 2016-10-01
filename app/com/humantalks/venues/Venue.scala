package com.humantalks.venues

import com.humantalks.common.models.values.{ Meta, GMapPlace }
import global.models.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Venue(
  id: Venue.Id,
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
    contact: Option[Venue.Contact],
    location: Option[GMapPlace],
    capacity: Option[Int], // nombre de place
    twitter: Option[String],
    logo: Option[String],
    comment: Option[String] // information supplémentaires
  )

  case class Contact(
    name: String,
    email: Option[String],
    phone: Option[String],
    comment: Option[String]
  )

  implicit val formatContact = Json.format[Venue.Contact]
  implicit val formatData = Json.format[Venue.Data]
  implicit val format = Json.format[Venue]
  val fieldsContact = mapping(
    "name" -> text.verifying("error.required", value => value.length > 0),
    "email" -> optional(email),
    "phone" -> optional(text),
    "comment" -> optional(text)
  )(Venue.Contact.apply)(Venue.Contact.unapply)
  val fields = mapping(
    "name" -> nonEmptyText,
    "contact" -> optional(Venue.fieldsContact),
    "location" -> optional(GMapPlace.fields),
    "capacity" -> optional(number),
    "twitter" -> optional(text),
    "logo" -> optional(text),
    "comment" -> optional(text)
  )(Venue.Data.apply)(Venue.Data.unapply)
}
