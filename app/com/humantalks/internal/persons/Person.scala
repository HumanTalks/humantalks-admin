package com.humantalks.internal.persons

import com.humantalks.common.values.Meta
import com.humantalks.common.services.TwitterSrv
import global.values.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Person(
  id: Person.Id,
  data: Person.Data,
  meta: Meta
)
object Person {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Person.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      name: String,
      twitter: Option[String],
      email: Option[String], // to match existing person when submiting a new talk
      phone: Option[String],
      avatar: Option[String],
      description: Option[String]
  ) {
    def trim: Data = this.copy(
      name = this.name.trim,
      twitter = this.twitter.map(TwitterSrv.toAccount),
      email = this.email.map(_.trim),
      phone = this.phone.map(_.trim),
      avatar = this.avatar.map(_.trim),
      description = this.description.map(_.trim)
    )
  }

  implicit val formatData = Json.format[Person.Data]
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "email" -> optional(email),
    "phone" -> optional(text),
    "avatar" -> optional(text),
    "description" -> optional(text)
  )(Person.Data.apply)(Person.Data.unapply)
}