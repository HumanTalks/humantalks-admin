package com.humantalks.common.services.slack.models

import play.api.libs.json.Json

case class SlackMessage(
  text: String,
  username: String,
  bot_id: String,
  `type`: String,
  subtype: String,
  t: Option[Double]
)
object SlackMessage {
  implicit val format = Json.format[SlackMessage]
}
