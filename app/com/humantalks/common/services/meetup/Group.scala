package com.humantalks.common.services.meetup

import org.joda.time.DateTime
import play.api.libs.json.Json

case class GroupCategory(
  id: Long,
  name: String,
  shortname: String,
  sort_name: String
)
case class GroupOrganizer(
  id: Long,
  name: String,
  bio: String,
  photo: Photo
)
case class Group(
  id: Long,
  link: String,
  name: String,
  urlname: String,
  category: GroupCategory,
  organizer: GroupOrganizer,
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
  group_photo: Photo,
  key_photo: Photo,
  photos: List[Photo],
  created: DateTime
)
object Group {
  implicit val formatCategory = Json.format[GroupCategory]
  implicit val formatOrganizer = Json.format[GroupOrganizer]
  implicit val format = Json.format[Group]
}
