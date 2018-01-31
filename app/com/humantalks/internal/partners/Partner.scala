package com.humantalks.internal.partners

import com.humantalks.common.services.TwitterSrv
import com.humantalks.common.values.{ GMapPlace, Meta }
import com.humantalks.internal.partners.Partner.Sponsor
import com.humantalks.internal.persons.Person
import global.helpers.EnumerationHelper
import global.helpers.FormHelper._
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.{ LocalDate, LocalTime }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Partner(
    id: Partner.Id,
    meetupRef: Option[Partner.MeetupRef],
    data: Partner.Data,
    meta: Meta
) {
  def isSponsor(date: LocalDate): Boolean =
    data.sponsoring.exists(s => s.start.isBefore(date) && s.end.isAfter(date))
  def lastSponsor(): Option[Sponsor] =
    data.sponsoring.sortBy(-_.end.toDate.getTime).headOption
}

object Partner {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Partner.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long)

  case class Data(
      name: String,
      twitter: Option[String],
      logo: Option[String],
      contacts: List[Person.Id],
      venue: Option[Venue],
      sponsoring: List[Sponsor],
      sponsorAperitif: Boolean,
      comment: Option[String]
  ) {
    lazy val allContacts: List[Person.Id] = contacts ++ venue.flatMap(_.contact).toList ++ sponsoring.flatMap(_.contact)
    def trim: Data = copy(
      name = name.trim,
      twitter = twitter.map(TwitterSrv.toAccount),
      logo = logo.map(_.trim),
      venue = venue.map(_.trim),
      comment = comment.map(_.trim)
    )
  }

  case class Venue(
      location: GMapPlace,
      capacity: Option[Int],
      closeTime: Option[LocalTime],
      attendeeList: Option[Boolean],
      entranceCheck: Option[Boolean],
      offeredAperitif: Option[Boolean],
      contact: Option[Person.Id],
      comment: Option[String]
  ) {
    def trim: Venue = copy(
      comment = comment.map(_.trim)
    )
  }

  case class Sponsor(
      start: LocalDate,
      end: LocalDate,
      level: SponsorLevel.Value,
      contact: Option[Person.Id]
  ) {
    def expireSoon(): Boolean =
      end.minusMonths(2).isBefore(new LocalDate())
    def expiredNotLongAgo(): Boolean =
      end.plusMonths(4).isAfter(new LocalDate())
  }

  object SponsorLevel extends Enumeration {
    val Standard, Premium = Value
  }
  implicit val formatSponsorLevel = EnumerationHelper.enumFormat(SponsorLevel)
  implicit val mappingSponsorLevel = EnumerationHelper.formMapping(SponsorLevel)

  implicit val formatSponsor = Json.format[Sponsor]
  implicit val formatVenue = Json.format[Venue]
  implicit val formatData = Json.format[Data]
  implicit val formatMeetupRef = Json.format[MeetupRef]
  implicit val format = Json.format[Partner]

  val sponsorFields = mapping(
    "start" -> jodaLocalDate("dd/MM/yyyy"),
    "end" -> jodaLocalDate("dd/MM/yyyy"),
    "level" -> of[SponsorLevel.Value],
    "contact" -> optional(of[Person.Id])
  )(Partner.Sponsor.apply)(Partner.Sponsor.unapply)
  val venueFields = mapping(
    "location" -> GMapPlace.fields,
    "capacity" -> optional(number),
    "closeTime" -> optional(jodaLocalTime("HH:mm")),
    "attendeeList" -> optional(boolean),
    "entranceCheck" -> optional(boolean),
    "offeredAperitif" -> optional(boolean),
    "contact" -> optional(of[Person.Id]),
    "comment" -> optional(text)
  )(Partner.Venue.apply)(Partner.Venue.unapply)
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "logo" -> optional(text),
    "contacts" -> list(of[Person.Id]),
    "venue" -> ignored(Option.empty[Venue]),
    "sponsoring" -> ignored(List[Sponsor]()),
    "sponsorAperitif" -> boolean,
    "comment" -> optional(text)
  )(Partner.Data.apply)(Partner.Data.unapply)
}
