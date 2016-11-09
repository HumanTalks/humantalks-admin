package com.humantalks.common.services.meetup

import org.joda.time.DateTime
import play.api.libs.json.Json

case class RsvpEvent(
  id: String,
  name: String,
  time: DateTime,
  utc_offset: Long,
  yes_rsvp_count: Int
)
case class RsvpGroup(
  id: Long,
  name: String,
  urlname: String,
  who: String,
  join_mode: String, // TODO enum (open)
  members: Int,
  group_photo: Photo
)
case class RsvpMemberContext(
  host: Boolean
)
case class RsvpMember(
  id: Long,
  name: String,
  photo: Option[Photo],
  event_context: RsvpMemberContext
)
case class Rsvp(
  member: RsvpMember,
  event: RsvpEvent,
  group: RsvpGroup,
  venue: Venue,
  response: String, // TODO enum (waitlist)
  guests: Int,
  created: DateTime,
  updated: DateTime
)
object Rsvp {
  implicit val formatEvent = Json.format[RsvpEvent]
  implicit val formatGroup = Json.format[RsvpGroup]
  implicit val formatMemberContext = Json.format[RsvpMemberContext]
  implicit val formatMember = Json.format[RsvpMember]
  implicit val format = Json.format[Rsvp]
}
