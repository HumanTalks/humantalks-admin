package com.humantalks.common.services.slack

import play.api.libs.json.Json

case class Message(
  text: String,
  username: String,
  bot_id: String,
  `type`: String,
  subtype: String,
  t: Option[Double]
)
object Message {
  implicit val format = Json.format[Message]
}
