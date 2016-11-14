package com.humantalks.common.services.meetup.models

case class VenueCreate(
    name: String,
    visibility: String, // TODO enum (private,public)
    address_1: String,
    address_2: Option[String],
    city: String,
    state: Option[String],
    country: String,
    web_url: Option[String],
    phone: Option[String],
    hours: Option[String]
) {
  def toParams: Map[String, String] = VenueCreate.toParams(this)
}
object VenueCreate {
  def toParams(data: VenueCreate): Map[String, String] =
    Map(
      "name" -> Some(data.name),
      "visibility" -> Some(data.visibility),
      "address_1" -> Some(data.address_1),
      "address_2" -> data.address_2,
      "city" -> Some(data.city),
      "state" -> data.state,
      "country" -> Some(data.country),
      "web_url" -> data.web_url,
      "phone" -> data.phone,
      "hours" -> data.hours
    ).flatMap { case (key, valueOpt) => valueOpt.map(value => (key, value)) }

  def from(venue: com.humantalks.internal.venues.Venue): Option[VenueCreate] = {
    val addressFromStreet = venue.data.location.flatMap(l => l.street.map(street => l.streetNo.map(_ + " ").getOrElse("") + street))
    val addressFromFormatted = venue.data.location.flatMap(_.formatted.split(",").headOption)
    val addressFromInput = venue.data.location.flatMap(_.input.split(",").drop(1).headOption)
    val cityOpt = venue.data.location.flatMap(_.locality)
    for {
      address <- addressFromStreet.orElse(addressFromFormatted).orElse(addressFromInput)
      city <- cityOpt
    } yield VenueCreate(
      name = venue.data.name,
      visibility = "public",
      address_1 = address,
      address_2 = None,
      city = city,
      state = None,
      country = venue.data.location.map(_.countryCode).getOrElse(""),
      web_url = venue.data.location.map(l => l.website.getOrElse(l.url)),
      phone = venue.data.location.flatMap(_.phone),
      hours = None
    )
  }
}
