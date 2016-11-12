package com.humantalks.common.values

import play.api.data.Forms._
import play.api.libs.json.Json

case class GMapPlace(
    id: String,
    name: String,
    streetNo: Option[String],
    street: Option[String],
    postalCode: Option[String],
    locality: Option[String],
    country: String,
    formatted: String,
    input: String,
    coords: Coords,
    url: String,
    website: Option[String],
    phone: Option[String]
) {
  def countryCode: String = country match {
    case "France" => "fr"
    case _ => ""
  }
}
object GMapPlace {
  implicit val format = Json.format[GMapPlace]
  val fields = mapping(
    "id" -> nonEmptyText,
    "name" -> nonEmptyText,
    "streetNo" -> optional(nonEmptyText),
    "street" -> optional(nonEmptyText),
    "postalCode" -> optional(nonEmptyText),
    "locality" -> optional(nonEmptyText),
    "country" -> nonEmptyText,
    "formatted" -> nonEmptyText,
    "input" -> nonEmptyText,
    "coords" -> Coords.fields,
    "url" -> nonEmptyText,
    "website" -> optional(nonEmptyText),
    "phone" -> optional(nonEmptyText)
  )(GMapPlace.apply)(GMapPlace.unapply)
}