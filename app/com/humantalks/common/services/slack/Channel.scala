package com.humantalks.common.services.slack

import play.api.libs.json.Json

case class ChannelTopic(
  value: String,
  creator: String,
  last_set: Long // timestamp in seconds
)
case class Channel(
  id: String,
  name: String,
  is_channel: Boolean,
  created: Long, // timestamp in seconds
  creator: String,
  is_archived: Boolean,
  is_general: Boolean,
  is_member: Boolean,
  members: List[String],
  topic: ChannelTopic,
  purpose: ChannelTopic,
  num_members: Option[Int]
)
object Channel {
  implicit val formatTopic = Json.format[ChannelTopic]
  implicit val format = Json.format[Channel]
}