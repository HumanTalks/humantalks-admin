package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.Partner
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicPartnerNoEvent(
  id: Partner.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String]
)
object PublicPartnerNoEvent {
  def from(partner: Partner): PublicPartnerNoEvent = PublicPartnerNoEvent(
    id = partner.id,
    name = partner.data.name,
    location = partner.data.venue.map(_.location),
    capacity = partner.data.venue.flatMap(_.capacity),
    twitter = partner.data.twitter,
    logo = partner.data.logo
  )

  implicit val format = Json.format[PublicPartnerNoEvent]
}

case class PublicEvent(
  id: Event.Id,
  title: String,
  date: DateTime,
  venueId: Option[Partner.Id],
  venue: Option[PublicPartnerNoEvent],
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String],
  personCount: Option[Int]
)
object PublicEvent {
  def from(event: Event, partnersOpt: Option[List[Partner]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicEvent = PublicEvent(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    venueId = if (partnersOpt.isDefined) None else event.data.venue,
    venue = partnersOpt.flatMap { partners =>
      event.data.venue.flatMap { venueId =>
        partners.find(_.id == venueId).map(PublicPartnerNoEvent.from)
      }
    },
    talkIds = if (talksOpt.isDefined) None else Some(event.data.talks),
    talks = talksOpt.map { talks =>
      event.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, eventsOpt = None, partnersOpt = None))
    },
    description = event.data.description,
    roti = event.data.roti,
    meetupUrl = event.meetupUrl,
    personCount = event.data.personCount
  )

  implicit val format = Json.format[PublicEvent]
}
