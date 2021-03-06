package com.humantalks.common.services.meetup.models

import play.api.libs.json.Json

case class MeetupPhoto(
  id: Long,
  `type`: String, // TODO enum (event, member)
  highres_link: Option[String],
  photo_link: String,
  thumb_link: String,
  base_url: String
)
object MeetupPhoto {
  implicit val format = Json.format[MeetupPhoto]
}
