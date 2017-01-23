package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicVenueNoEvent(
  id: Venue.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String]
)
object PublicVenueNoEvent {
  def from(venue: Venue): PublicVenueNoEvent = PublicVenueNoEvent(
    id = venue.id,
    name = venue.data.name,
    location = venue.data.location,
    capacity = venue.data.capacity,
    twitter = venue.data.twitter,
    logo = venue.data.logo
  )

  implicit val format = Json.format[PublicVenueNoEvent]
}

case class PublicEvent(
  id: Event.Id,
  title: String,
  date: DateTime,
  venueId: Option[Venue.Id],
  venue: Option[PublicVenueNoEvent],
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String],
  personCount: Option[Int]
)
object PublicEvent {
  def from(event: Event, venuesOpt: Option[List[Venue]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicEvent = PublicEvent(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    venueId = if (venuesOpt.isDefined) None else event.data.venue,
    venue = venuesOpt.flatMap { venues =>
      event.data.venue.flatMap { venueId =>
        venues.find(_.id == venueId).map(PublicVenueNoEvent.from)
      }
    },
    talkIds = if (talksOpt.isDefined) None else Some(event.data.talks),
    talks = talksOpt.map { talks =>
      event.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, eventsOpt = None, venuesOpt = None))
    },
    description = event.data.description,
    roti = event.data.roti,
    meetupUrl = event.meetupUrl,
    personCount = event.data.personCount
  )

  implicit val format = Json.format[PublicEvent]
}
