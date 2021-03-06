package com.humantalks.common.services.meetup.models

import com.humantalks.internal.events.Event
import com.humantalks.internal.partners.Partner
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import org.joda.time.DateTime

case class MeetupEventCreate(
    status: String, // TODO enum (draft, cancelled, suggested, proposed, upcoming, past)
    name: String, // < 80
    description: String, // < 50000
    time: DateTime,
    duration: Option[Int], // in millis
    rsvp_limit: Option[Int],
    guest_limit: Int,
    rsvp_open_time: DateTime,
    rsvp_close_time: DateTime,
    hosts: List[Long],
    self_rsvp: Boolean,
    venue_id: Option[Long],
    venue_visibility: Option[String], // TODO enum (public, members)
    lat: Option[Double],
    lon: Option[Double],
    how_to_find_us: Option[String],
    question: Option[String] // < 250
) {
  def toParams: Map[String, String] = MeetupEventCreate.toParams(this)
}
object MeetupEventCreate {
  val lkn = 14321102L
  val mpa = 27217322L
  val sbo = 92525632L
  val tca = 33725712L
  val ape = 185927921L
  val hosts = List(lkn, mpa, sbo, tca, ape)

  def toParams(data: MeetupEventCreate): Map[String, String] =
    Map(
      "publish_status" -> Some(data.status),
      "name" -> Some(data.name),
      "description" -> Some(data.description),
      "time" -> Some(data.time.getMillis.toString),
      "duration" -> data.duration.map(_.toString),
      "rsvp_limit" -> data.rsvp_limit.map(_.toString),
      "guest_limit" -> Some(data.guest_limit.toString),
      "rsvp_open_time" -> Some(data.rsvp_open_time.getMillis.toString),
      "rsvp_close_time" -> Some(data.rsvp_close_time.getMillis.toString),
      "event_hosts" -> Some(data.hosts.mkString(",")),
      "self_rsvp" -> Some(data.self_rsvp.toString),
      "venue_id" -> data.venue_id.map(_.toString),
      "venue_visibility" -> data.venue_visibility,
      "lat" -> data.lat.map(_.toString),
      "lon" -> data.lon.map(_.toString),
      "how_to_find_us" -> data.how_to_find_us,
      "question" -> data.question
    ).flatMap { case (key, valueOpt) => valueOpt.map(value => (key, value)) }

  def from(event: Event, description: String, partnerOpt: Option[Partner], talkList: List[Talk], personList: List[Person]): MeetupEventCreate =
    MeetupEventCreate(
      status = "draft",
      name = event.data.title,
      description = description,
      time = event.data.date,
      duration = None,
      rsvp_limit = partnerOpt.flatMap(_.data.venue).flatMap(_.capacity),
      guest_limit = 0,
      rsvp_open_time = new DateTime(0),
      rsvp_close_time = new DateTime(0),
      hosts = hosts,
      self_rsvp = true,
      venue_id = partnerOpt.flatMap(_.meetupRef).map(_.id),
      venue_visibility = partnerOpt.flatMap(_.meetupRef).map(_ => "public"),
      lat = partnerOpt.flatMap(_.data.venue).map(_.location.coords.lat),
      lon = partnerOpt.flatMap(_.data.venue).map(_.location.coords.lng),
      how_to_find_us = None,
      question = None
    )
}
