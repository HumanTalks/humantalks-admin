package com.humantalks.domain.values

import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.libs.json.Json

case class Coords(
  lat: Double,
  lng: Double
)
object Coords {
  implicit val format = Json.format[Coords]
  val fields = mapping(
    "lat" -> of(doubleFormat),
    "lng" -> of(doubleFormat)
  )(Coords.apply)(Coords.unapply)
}
