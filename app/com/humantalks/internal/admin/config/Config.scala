package com.humantalks.internal.admin.config

import com.humantalks.common.values.Meta
import global.helpers.EnumerationHelper
import global.values.{ TypedIdHelper, TypedId }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Config(
  id: Config.Id,
  data: Config.Data,
  meta: Meta
)
object Config {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Config.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      ref: String,
      content: ContentType.Value,
      description: String,
      value: String
  ) {
    def trim: Data = this.copy(
      ref = ref.trim,
      description = description.trim,
      value = value.trim
    )
  }

  object ContentType extends Enumeration {
    val Text, Markdown, Html = Value
  }
  implicit val mappingContentType = EnumerationHelper.formMapping(ContentType)
  implicit val formatContentType = EnumerationHelper.enumFormat(ContentType)
  implicit val formatData = Json.format[Data]
  implicit val format = Json.format[Config]
  val fields = mapping(
    "ref" -> nonEmptyText,
    "content" -> of[ContentType.Value],
    "description" -> nonEmptyText,
    "value" -> nonEmptyText
  )(Data.apply)(Data.unapply)
}
