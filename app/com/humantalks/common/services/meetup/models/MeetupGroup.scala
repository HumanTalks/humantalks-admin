package com.humantalks.common.services.meetup.models

import org.joda.time.DateTime
import play.api.libs.json.Json

case class MeetupGroupCategory(
  id: Long,
  name: String,
  shortname: String,
  sort_name: String
)
case class MeetupGroupOrganizer(
  id: Long,
  name: String,
  bio: String,
  photo: MeetupPhoto
)
case class MeetupGroup(
                        id: Long,
                        link: String,
                        name: String,
                        urlname: String,
                        category: MeetupGroupCategory,
                        organizer: MeetupGroupOrganizer,
                        description: String,
                        join_mode: String, // TODO enum (open)
                        visibility: String, // TODO enum (public)
                        members: Int,
                        who: String,
                        lat: Double,
                        lon: Double,
                        city: String,
                        state: String,
                        country: String,
                        localized_country_name: String,
                        timezone: String,
                        group_photo: MeetupPhoto,
                        key_photo: MeetupPhoto,
                        photos: List[MeetupPhoto],
                        created: DateTime
)
object MeetupGroup {
  implicit val formatCategory = Json.format[MeetupGroupCategory]
  implicit val formatOrganizer = Json.format[MeetupGroupOrganizer]
  implicit val format = Json.format[MeetupGroup]
}
