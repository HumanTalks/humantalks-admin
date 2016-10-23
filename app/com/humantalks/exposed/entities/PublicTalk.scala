package com.humantalks.exposed.entities

import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicMeetupNoTalks(
  id: Meetup.Id,
  title: String,
  date: DateTime,
  venueId: Option[Venue.Id],
  venue: Option[PublicVenueNoMeetup],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicMeetupNoTalks {
  def from(meetup: Meetup, venuesOpt: Option[List[Venue]]): PublicMeetupNoTalks = PublicMeetupNoTalks(
    id = meetup.id,
    title = meetup.data.title,
    date = meetup.data.date,
    venueId = if (venuesOpt.isDefined) None else meetup.data.venue,
    venue = venuesOpt.flatMap { venues =>
      meetup.data.venue.flatMap { venueId =>
        venues.find(_.id == venueId).map(PublicVenueNoMeetup.from)
      }
    },
    description = meetup.data.description,
    roti = meetup.data.roti,
    meetupUrl = meetup.data.meetupUrl
  )

  implicit val format = Json.format[PublicMeetupNoTalks]
}

case class PublicTalk(
  id: Talk.Id,
  title: String,
  description: Option[String],
  speakerIds: Option[List[Person.Id]],
  speakers: Option[List[PublicPerson]],
  slides: Option[String],
  slidesEmbedCode: Option[String],
  video: Option[String],
  videoEmbedCode: Option[String],
  meetup: Option[PublicMeetupNoTalks]
)
object PublicTalk {
  def from(talk: Talk, speakersOpt: Option[List[Person]], meetupsOpt: Option[List[Meetup]], venuesOpt: Option[List[Venue]]): PublicTalk = PublicTalk(
    id = talk.id,
    title = talk.data.title,
    description = talk.data.description,
    speakerIds = if (speakersOpt.isDefined) None else Some(talk.data.speakers),
    speakers = speakersOpt.map { speakers =>
      talk.data.speakers.flatMap(id => speakers.find(_.id == id)).map(speaker => PublicPerson.from(speaker, talksOpt = None, meetupsOpt = None, venuesOpt = None))
    },
    slides = talk.data.slides,
    slidesEmbedCode = talk.data.slidesEmbedCode,
    video = talk.data.video,
    videoEmbedCode = talk.data.videoEmbedCode,
    meetup = meetupsOpt.flatMap { meetups =>
      meetups.find(_.data.talks.contains(talk.id)).map(meetup => PublicMeetupNoTalks.from(meetup, venuesOpt))
    }
  )

  implicit val format = Json.format[PublicTalk]
}
