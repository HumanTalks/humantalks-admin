package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicEventNoVenue(
  id: Event.Id,
  title: String,
  date: DateTime,
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicEventNoVenue {
  def from(event: Event, talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicEventNoVenue = PublicEventNoVenue(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    talkIds = if (talksOpt.isDefined) None else Some(event.data.talks),
    talks = talksOpt.map { talks =>
      event.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, eventsOpt = None, venuesOpt = None))
    },
    description = event.data.description,
    roti = event.data.roti,
    meetupUrl = event.meetupUrl
  )

  implicit val format = Json.format[PublicEventNoVenue]
}

case class PublicVenue(
  id: Venue.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String],
  meetups: Option[List[PublicEventNoVenue]]
)
object PublicVenue {
  def from(venue: Venue, eventsOpt: Option[List[Event]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicVenue = PublicVenue(
    id = venue.id,
    name = venue.data.name,
    location = venue.data.location,
    capacity = venue.data.capacity,
    twitter = venue.data.twitter,
    logo = venue.data.logo,
    meetups = eventsOpt.map { event =>
      event.filter(_.data.venue.contains(venue.id)).map(event => PublicEventNoVenue.from(event, talksOpt, speakersOpt))
    }
  )

  implicit val format = Json.format[PublicVenue]
}
