package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicVenueNoMeetup(
  id: Venue.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String]
)
object PublicVenueNoMeetup {
  def from(venue: Venue): PublicVenueNoMeetup = PublicVenueNoMeetup(
    id = venue.id,
    name = venue.data.name,
    location = venue.data.location,
    capacity = venue.data.capacity,
    twitter = venue.data.twitter,
    logo = venue.data.logo
  )

  implicit val format = Json.format[PublicVenueNoMeetup]
}

case class PublicMeetup(
  id: Meetup.Id,
  title: String,
  date: DateTime,
  venueId: Option[Venue.Id],
  venue: Option[PublicVenueNoMeetup],
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicMeetup {
  def from(meetup: Meetup, venuesOpt: Option[List[Venue]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicMeetup = PublicMeetup(
    id = meetup.id,
    title = meetup.data.title,
    date = meetup.data.date,
    venueId = if (venuesOpt.isDefined) None else meetup.data.venue,
    venue = venuesOpt.flatMap { venues =>
      meetup.data.venue.flatMap { venueId =>
        venues.find(_.id == venueId).map(PublicVenueNoMeetup.from)
      }
    },
    talkIds = if (talksOpt.isDefined) None else Some(meetup.data.talks),
    talks = talksOpt.map { talks =>
      meetup.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, meetupsOpt = None, venuesOpt = None))
    },
    description = meetup.data.description,
    roti = meetup.data.roti,
    meetupUrl = meetup.data.meetupUrl
  )

  implicit val format = Json.format[PublicMeetup]
}
