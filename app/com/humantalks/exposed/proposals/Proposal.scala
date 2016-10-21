package com.humantalks.exposed.proposals

import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import global.values.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Proposal(
  id: Proposal.Id,
  data: Proposal.Data,
  talk: Option[Talk.Id],
  meta: Meta
)
object Proposal {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Proposal.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      title: String,
      description: Option[String],
      speakers: List[Person.Id],
      slides: Option[String],
      slidesEmbedCode: Option[String]
  ) {
    def trim: Data = copy(
      title = title.trim,
      description = description.map(_.trim),
      slides = slides.map(_.trim)
    )
    def toTalk: Talk.Data = Talk.Data(title, description, speakers, slides, slidesEmbedCode, None, None)
  }

  implicit val formatData = Json.format[Proposal.Data]
  implicit val format = Json.format[Proposal]
  val fields = mapping(
    "title" -> nonEmptyText,
    "description" -> optional(text),
    "speakers" -> list(of[Person.Id]),
    "slides" -> optional(text),
    "slidesEmbedCode" -> ignored(Option.empty[String])
  )(Proposal.Data.apply)(Proposal.Data.unapply)
}
