package com.humantalks.talks

import com.humantalks.common.models.values.Meta
import com.humantalks.persons.Person
import global.models.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Talk(
  id: Talk.Id,
  data: Talk.Data,
  meta: Meta
)
object Talk {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Talk.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
    title: String,
    description: String,
    speakers: List[Person.Id],
    slides: Option[String],
    video: Option[String]
  )

  implicit val formatData = Json.format[Talk.Data]
  implicit val format = Json.format[Talk]
  val fields = mapping(
    "title" -> nonEmptyText,
    "description" -> nonEmptyText,
    "speakers" -> list(of[Person.Id]),
    "slides" -> optional(text),
    "video" -> optional(text)
  )(Talk.Data.apply)(Talk.Data.unapply)
}
