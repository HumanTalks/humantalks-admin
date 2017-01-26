package com.humantalks.common.services.meetup.models

import com.humantalks.internal.partners.Partner

case class MeetupVenueCreate(
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
  def toParams: Map[String, String] = MeetupVenueCreate.toParams(this)
}
object MeetupVenueCreate {
  def toParams(data: MeetupVenueCreate): Map[String, String] =
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

  def from(partner: Partner): Option[MeetupVenueCreate] = {
    val addressFromStreet = partner.data.venue.flatMap(v => v.location.street.map(street => v.location.streetNo.map(_ + " ").getOrElse("") + street))
    val addressFromFormatted = partner.data.venue.flatMap(_.location.formatted.split(",").headOption)
    val addressFromInput = partner.data.venue.flatMap(_.location.input.split(",").drop(1).headOption)
    val cityOpt = partner.data.venue.flatMap(_.location.locality)
    for {
      address <- addressFromStreet.orElse(addressFromFormatted).orElse(addressFromInput)
      city <- cityOpt
    } yield MeetupVenueCreate(
      name = partner.data.name,
      visibility = "public",
      address_1 = address,
      address_2 = None,
      city = city,
      state = None,
      country = partner.data.venue.map(_.location.countryCode).getOrElse(""),
      web_url = partner.data.venue.map(v => v.location.website.getOrElse(v.location.url)),
      phone = partner.data.venue.flatMap(_.location.phone),
      hours = None
    )
  }
}
