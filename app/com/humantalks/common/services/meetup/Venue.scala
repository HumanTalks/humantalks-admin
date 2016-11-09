package com.humantalks.common.services.meetup

import play.api.libs.json.Json

case class Venue(
  id: Long,
  name: String,
  visibility: Option[String], // TODO enum (public,private)
  repinned: Option[Boolean],
  phone: Option[String],
  lat: Double,
  lon: Double,
  address_1: String,
  address_2: Option[String],
  address_3: Option[String],
  city: String,
  zip: Option[String],
  state: Option[String],
  country: String,
  localized_country_name: String
)
object Venue {
  implicit val format = Json.format[Venue]
}