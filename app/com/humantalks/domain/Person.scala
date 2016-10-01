package com.humantalks.domain

import com.humantalks.domain.values.Meta
import global.models.{ TypedIdHelper, TypedId }
import play.api.data.Forms._
import play.api.libs.json.Json

case class PersonData(
  name: String,
  twitter: Option[String],
  email: Option[String], // to match existing person when submiting a new talk
  avatar: Option[String],
  description: Option[String]
)
object PersonData {
  implicit val format = Json.format[PersonData]
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "email" -> optional(email),
    "avatar" -> optional(text),
    "description" -> optional(text)
  )(PersonData.apply)(PersonData.unapply)
}

case class Person(
  id: Person.Id,
  data: PersonData,
  meta: Meta
)
object Person {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Person.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  implicit val format = Json.format[Person]
}