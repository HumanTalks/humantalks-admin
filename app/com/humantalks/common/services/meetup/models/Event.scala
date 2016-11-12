package com.humantalks.common.services.meetup.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class EventGroup(
  id: Long,
  name: String,
  urlname: String,
  who: String,
  join_mode: String, // TODO enum (open)
  lat: Double,
  lon: Double,
  created: DateTime
)
case class Event(
  id: String,
  link: String,
  name: String,
  description: String,
  status: String, // TODO enum (draft,upcoming,past)
  visibility: String, // TODO enum (public)
  time: DateTime,
  utc_offset: Long,
  duration: Option[Int],
  rsvp_limit: Option[Int],
  yes_rsvp_count: Int,
  waitlist_count: Int,
  venue: Venue,
  group: EventGroup,
  created: DateTime,
  updated: DateTime
)
object Event {
  implicit val formatGroup = Json.format[EventGroup]
  implicit val format = Json.format[Event]
}
