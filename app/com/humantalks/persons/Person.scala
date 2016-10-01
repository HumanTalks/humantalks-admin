package com.humantalks.persons

import com.humantalks.common.models.values.Meta
import global.models.{ TypedId, TypedIdHelper }
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
    avatar: Option[String],
    description: Option[String]
  )

  implicit val formatData = Json.format[Person.Data]
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "email" -> optional(email),
    "avatar" -> optional(text),
    "description" -> optional(text)
  )(Person.Data.apply)(Person.Data.unapply)
}