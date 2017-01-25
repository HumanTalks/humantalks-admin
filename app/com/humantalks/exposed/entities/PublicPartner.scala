package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.Partner
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicEventNoPartner(
  id: Event.Id,
  title: String,
  date: DateTime,
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicEventNoPartner {
  def from(event: Event, talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicEventNoPartner = PublicEventNoPartner(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    talkIds = if (talksOpt.isDefined) None else Some(event.data.talks),
    talks = talksOpt.map { talks =>
      event.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, eventsOpt = None, partnersOpt = None))
    },
    description = event.data.description,
    roti = event.data.roti,
    meetupUrl = event.meetupUrl
  )

  implicit val format = Json.format[PublicEventNoPartner]
}

case class PublicPartner(
  id: Partner.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String],
  meetups: Option[List[PublicEventNoPartner]]
)
object PublicPartner {
  def from(partner: Partner, eventsOpt: Option[List[Event]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicPartner = PublicPartner(
    id = partner.id,
    name = partner.data.name,
    location = partner.data.location,
    capacity = partner.data.capacity,
    twitter = partner.data.twitter,
    logo = partner.data.logo,
    meetups = eventsOpt.map { event =>
      event.filter(_.data.venue.contains(partner.id)).map(event => PublicEventNoPartner.from(event, talksOpt, speakersOpt))
    }
  )

  implicit val format = Json.format[PublicPartner]
}
