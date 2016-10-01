package com.humantalks.domain

import com.humantalks.domain.values.Meta
import global.models.{ TypedIdHelper, TypedId }
import play.api.data.Forms._
import play.api.libs.json.Json

case class TalkData(
  title: String,
  description: String,
  speakers: List[Person.Id],
  slides: Option[String],
  video: Option[String]
)
object TalkData {
  implicit val format = Json.format[TalkData]
  val fields = mapping(
    "title" -> nonEmptyText,
    "description" -> nonEmptyText,
    "speakers" -> list(of[Person.Id]),
    "slides" -> optional(text),
    "video" -> optional(text)
  )(TalkData.apply)(TalkData.unapply)
}

case class Talk(
  id: Talk.Id,
  data: TalkData,
  meta: Meta
)
object Talk {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Talk.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  implicit val format = Json.format[Talk]
}
