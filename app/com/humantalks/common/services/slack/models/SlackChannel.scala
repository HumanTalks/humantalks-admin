package com.humantalks.common.services.slack.models

import play.api.libs.json.Json

case class SlackChannelTopic(
  value: String,
  creator: String,
  last_set: Long // timestamp in seconds
)
case class SlackChannel(
  id: String,
  name: String,
  is_channel: Boolean,
  created: Long, // timestamp in seconds
  creator: String,
  is_archived: Boolean,
  is_general: Boolean,
  is_member: Boolean,
  members: List[String],
  topic: SlackChannelTopic,
  purpose: SlackChannelTopic,
  num_members: Option[Int]
)
object SlackChannel {
  implicit val formatTopic = Json.format[SlackChannelTopic]
  implicit val format = Json.format[SlackChannel]
}