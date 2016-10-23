package com.humantalks.exposed.entities

import com.humantalks.common.values.GMapPlace
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicMeetupNoVenue(
  id: Meetup.Id,
  title: String,
  date: DateTime,
  talkIds: Option[List[Talk.Id]],
  talks: Option[List[PublicTalk]],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicMeetupNoVenue {
  def from(meetup: Meetup, talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicMeetupNoVenue = PublicMeetupNoVenue(
    id = meetup.id,
    title = meetup.data.title,
    date = meetup.data.date,
    talkIds = if (talksOpt.isDefined) None else Some(meetup.data.talks),
    talks = talksOpt.map { talks =>
      meetup.data.talks.flatMap(id => talks.find(_.id == id)).map(talk => PublicTalk.from(talk, speakersOpt, meetupsOpt = None, venuesOpt = None))
    },
    description = meetup.data.description,
    roti = meetup.data.roti,
    meetupUrl = meetup.data.meetupUrl
  )

  implicit val format = Json.format[PublicMeetupNoVenue]
}

case class PublicVenue(
  id: Venue.Id,
  name: String,
  location: Option[GMapPlace],
  capacity: Option[Int],
  twitter: Option[String],
  logo: Option[String],
  meetups: Option[List[PublicMeetupNoVenue]]
)
object PublicVenue {
  def from(venue: Venue, meetupsOpt: Option[List[Meetup]], talksOpt: Option[List[Talk]], speakersOpt: Option[List[Person]]): PublicVenue = PublicVenue(
    id = venue.id,
    name = venue.data.name,
    location = venue.data.location,
    capacity = venue.data.capacity,
    twitter = venue.data.twitter,
    logo = venue.data.logo,
    meetups = meetupsOpt.map { meetups =>
      meetups.filter(_.data.venue.contains(venue.id)).map(meetup => PublicMeetupNoVenue.from(meetup, talksOpt, speakersOpt))
    }
  )

  implicit val format = Json.format[PublicVenue]
}
