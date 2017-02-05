package com.humantalks.common.services.meetup.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class MeetupRsvpEvent(
  id: String,
  name: String,
  time: DateTime,
  utc_offset: Long,
  yes_rsvp_count: Int
)
case class MeetupRsvpGroup(
  id: Long,
  name: String,
  urlname: String,
  who: String,
  join_mode: String, // TODO enum (open)
  members: Int,
  group_photo: MeetupPhoto
)
case class MeetupRsvpMemberContext(
  host: Boolean
)
case class MeetupRsvpMember(
  id: Long,
  name: String,
  photo: Option[MeetupPhoto],
  event_context: MeetupRsvpMemberContext
)
case class MeetupRsvp(
  member: MeetupRsvpMember,
  event: MeetupRsvpEvent,
  group: MeetupRsvpGroup,
  venue: MeetupVenue,
  response: String, // TODO enum (yes, no, waitlist)
  guests: Int,
  created: DateTime,
  updated: DateTime
)
object MeetupRsvp {
  implicit val formatEvent = Json.format[MeetupRsvpEvent]
  implicit val formatGroup = Json.format[MeetupRsvpGroup]
  implicit val formatMemberContext = Json.format[MeetupRsvpMemberContext]
  implicit val formatMember = Json.format[MeetupRsvpMember]
  implicit val format = Json.format[MeetupRsvp]
}
