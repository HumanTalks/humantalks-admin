package com.humantalks.common.services.meetup.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class MeetupEventGroup(
  id: Long,
  name: String,
  urlname: String,
  who: String,
  join_mode: String, // TODO enum (open)
  lat: Double,
  lon: Double,
  created: DateTime
)
case class MeetupEvent(
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
                        venue: MeetupVenue,
                        group: MeetupEventGroup,
                        created: DateTime,
                        updated: DateTime
)
object MeetupEvent {
  implicit val formatGroup = Json.format[MeetupEventGroup]
  implicit val format = Json.format[MeetupEvent]
}
