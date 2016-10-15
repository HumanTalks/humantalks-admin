package com.humantalks.common.services.sendgrid

import play.api.libs.json.Json

case class Content(
  `type`: String, // text/plain, text/html...
  value: String
)
object Content {
  implicit val format = Json.format[Content]
  def text(value: String): Content = Content("text/plain", value)
  def html(value: String): Content = Content("text/html", value)
}

case class Address(
  email: String,
  name: Option[String] = None
)
object Address {
  implicit val format = Json.format[Address]
}

case class Recipient(
  to: Seq[Address],
  cc: Option[Seq[Address]] = None,
  bcc: Option[Seq[Address]] = None,
  substitutions: Option[Map[String, String]] = None,
  subject: Option[String] = None
)
object Recipient {
  implicit val format = Json.format[Recipient]
  def single(email: String): Seq[Recipient] = Seq(Recipient(to = Seq(Address(email))))
}

case class Email(
  personalizations: Seq[Recipient],
  from: Address,
  reply_to: Option[Address] = None,
  subject: String,
  content: Seq[Content]
)
object Email {
  implicit val format = Json.format[Email]
}