package com.humantalks.internal.talks

import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import global.helpers.EnumerationHelper
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.LocalDate
import play.api.data.Forms._
import play.api.libs.json.Json

case class Talk(
  id: Talk.Id,
  status: Talk.Status.Value,
  data: Talk.Data,
  meta: Meta
)
object Talk {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Talk.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  object Status extends Enumeration {
    val Proposed, Accepted, Planified, Finalized, Rejected = Value
  }
  implicit val statusPathBinder = EnumerationHelper.pathBinder(Status)
  implicit val statusQueryBinderList = EnumerationHelper.queryBinderList(Status)
  implicit val statusFormat = EnumerationHelper.enumFormat(Status)

  case class Data(
      title: String,
      description: Option[String],
      speakers: List[Person.Id],
      slides: Option[String],
      slidesEmbedCode: Option[String],
      video: Option[String],
      videoEmbedCode: Option[String],
      proposal: Option[Proposal]
  ) {
    def trim: Data = copy(
      title = title.trim,
      description = description.map(_.trim),
      slides = slides.map(_.trim),
      video = video.map(_.trim)
    )
  }

  case class Proposal(
    availabilities: List[LocalDate]
  )

  implicit val formatProposal = Json.format[Proposal]
  implicit val formatData = Json.format[Data]
  implicit val format = Json.format[Talk]
  val fieldsProposal = mapping(
    "availabilities" -> list(jodaLocalDate(pattern = "dd/MM/yyyy"))
  )(Proposal.apply)(Proposal.unapply)
  val fields = mapping(
    "title" -> nonEmptyText,
    "description" -> optional(text),
    "speakers" -> list(of[Person.Id]),
    "slides" -> optional(text),
    "slidesEmbedCode" -> ignored(Option.empty[String]),
    "video" -> optional(text),
    "videoEmbedCode" -> ignored(Option.empty[String]),
    "proposal" -> optional(fieldsProposal)
  )(Talk.Data.apply)(Talk.Data.unapply)
}
