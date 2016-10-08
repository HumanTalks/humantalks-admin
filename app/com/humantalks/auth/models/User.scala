package com.humantalks.auth.models

import global.models.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.libs.json.Json

case class User(
  id: User.Id,
  email: String,
  created: DateTime,
  updated: DateTime
)
object User {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "User.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }
  val fake = User.Id("57b2edc0-3d2f-4cb3-94d0-b60c028738a4")

  implicit val format = Json.format[User]
}
