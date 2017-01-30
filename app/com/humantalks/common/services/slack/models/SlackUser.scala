package com.humantalks.common.services.slack.models

import play.api.libs.json.Json

case class SlackUserProfile(
  real_name: String,
  first_name: Option[String],
  last_name: Option[String],
  email: Option[String],
  phone: Option[String],
  skype: Option[String],
  avatar_hash: String,
  image_24: String,
  image_32: String,
  image_48: String,
  image_72: String,
  image_192: String
)
case class SlackUser(
  id: String,
  team_id: String,
  name: String,
  real_name: Option[String],
  profile: SlackUserProfile,
  color: Option[String],
  status: Option[String],
  is_admin: Option[Boolean],
  is_owner: Option[Boolean],
  has_2fa: Option[Boolean],
  deleted: Boolean,
  tz: Option[String],
  tz_label: Option[String],
  tz_offset: Option[Int]
)
object SlackUser {
  implicit val formatProfile = Json.format[SlackUserProfile]
  implicit val format = Json.format[SlackUser]
}
