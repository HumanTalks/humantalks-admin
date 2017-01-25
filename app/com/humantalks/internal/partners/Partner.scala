package com.humantalks.internal.partners

import com.humantalks.common.values.{ GMapPlace, Meta }
import com.humantalks.common.services.TwitterSrv
import global.values.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json
import com.humantalks.internal.persons.Person

case class Partner(
  id: Partner.Id,
  meetupRef: Option[Partner.MeetupRef],
  data: Partner.Data,
  meta: Meta
)
object Partner {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Partner.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long)

  case class Data(
      name: String, // nom de la société / salle
      location: Option[GMapPlace],
      capacity: Option[Int], // nombre de places
      twitter: Option[String],
      logo: Option[String],
      contacts: List[Person.Id],
      comment: Option[String] // information supplémentaires
  ) {
    def trim: Data = copy(
      name = name.trim,
      twitter = twitter.map(TwitterSrv.toAccount),
      logo = logo.map(_.trim),
      comment = comment.map(_.trim)
    )
  }

  implicit val formatData = Json.format[Data]
  implicit val formatMeetupRef = Json.format[MeetupRef]
  implicit val format = Json.format[Partner]
  val fields = mapping(
    "name" -> nonEmptyText,
    "location" -> optional(GMapPlace.fields),
    "capacity" -> optional(number),
    "twitter" -> optional(text),
    "logo" -> optional(text),
    "contacts" -> list(of[Person.Id]),
    "comment" -> optional(text)
  )(Partner.Data.apply)(Partner.Data.unapply)
}
