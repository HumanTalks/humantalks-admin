package com.humantalks.exposed.proposals

import com.humantalks.internal.persons.Person
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Proposal(
  id: Proposal.Id,
  data: Proposal.Data,
  created: DateTime,
  updated: DateTime
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
      slidesEmbedCode: Option[String],
      video: Option[String],
      videoEmbedCode: Option[String]
  ) {
    def trim: Data = this.copy(
      title = this.title.trim,
      description = this.description.map(_.trim),
      slides = this.slides.map(_.trim),
      video = this.video.map(_.trim)
    )
  }

  implicit val formatData = Json.format[Proposal.Data]
  implicit val format = Json.format[Proposal]
  val fields = mapping(
    "title" -> nonEmptyText,
    "description" -> optional(text),
    "speakers" -> list(of[Person.Id]),
    "slides" -> optional(text),
    "slidesEmbedCode" -> ignored(Option.empty[String]),
    "video" -> optional(text),
    "videoEmbedCode" -> ignored(Option.empty[String])
  )(Proposal.Data.apply)(Proposal.Data.unapply)
}
