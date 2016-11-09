package com.humantalks.common.services.meetup

import org.joda.time.DateTime

// cf https://www.meetup.com/fr-FR/meetup_api/docs/:urlname/events/?uri=%2Fmeetup_api%2Fdocs%2F%3Aurlname%2Fevents%2F#create
case class EventCreate(
    announce: Boolean,
    status: String, // TODO enum (draft)
    name: String, // < 80
    description: String, // < 50000
    time: DateTime,
    //duration: Int, // in millis
    rsvp_limit: Int,
    guest_limit: Int,
    rsvp_open_time: DateTime,
    rsvp_close_time: DateTime,
    hosts: List[Long],
    self_rsvp: Boolean,
    venue_id: Long,
    venue_visibility: String, // TODO enum (public, members)
    lat: Double,
    lon: Double,
    how_to_find_us: String,
    question: String // < 250
) {
  def toParams: Map[String, String] = EventCreate.toParams(this)
}
object EventCreate {
  val lkn = 14321102
  val mpa = 27217322
  val sbo = 92525632
  val tca = 33725712
  val ape = 185927921
  val hosts = List(lkn, mpa, sbo, tca, ape)
  val google = 24856453

  def toParams(data: EventCreate): Map[String, String] = Map(
    "announce" -> data.announce.toString,
    "publish_status" -> data.status,
    "name" -> data.name,
    "description" -> data.description,
    "time" -> data.time.getMillis.toString,
    //"duration" -> data.duration.toString,
    "rsvp_limit" -> data.rsvp_limit.toString,
    "guest_limit" -> data.guest_limit.toString,
    "rsvp_open_time" -> data.rsvp_open_time.getMillis.toString,
    "rsvp_close_time" -> data.rsvp_close_time.getMillis.toString,
    "event_hosts" -> data.hosts.mkString(","),
    "self_rsvp" -> data.self_rsvp.toString,
    "venue_id" -> data.venue_id.toString,
    "venue_visibility" -> data.venue_visibility,
    "lat" -> data.lat.toString,
    "lon" -> data.lon.toString,
    "how_to_find_us" -> data.how_to_find_us,
    "question" -> data.question
  )
  def from(meetup: com.humantalks.internal.meetups.Meetup, venue: com.humantalks.internal.venues.Venue, hosts: List[Long], announce: Boolean): EventCreate = {
    EventCreate(
      announce = announce,
      status = "draft",
      name = meetup.data.title,
      description = meetup.data.description.getOrElse(""),
      time = meetup.data.date,
      //duration = 3 * 60 * 60 * 1000,
      rsvp_limit = venue.data.capacity.getOrElse(0),
      guest_limit = 0,
      rsvp_open_time = new DateTime(0),
      rsvp_close_time = new DateTime(0),
      hosts = hosts,
      self_rsvp = true,
      venue_id = venue.meetupId.get,
      venue_visibility = "public",
      lat = venue.data.location.get.coords.lat,
      lon = venue.data.location.get.coords.lng,
      how_to_find_us = "",
      question = ""
    )
  }
}
