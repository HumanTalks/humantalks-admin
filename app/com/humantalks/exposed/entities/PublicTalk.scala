package com.humantalks.exposed.entities

import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicEventNoTalks(
  id: Event.Id,
  title: String,
  date: DateTime,
  venueId: Option[Venue.Id],
  venue: Option[PublicVenueNoEvent],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicEventNoTalks {
  def from(event: Event, venuesOpt: Option[List[Venue]]): PublicEventNoTalks = PublicEventNoTalks(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    venueId = if (venuesOpt.isDefined) None else event.data.venue,
    venue = venuesOpt.flatMap { venues =>
      event.data.venue.flatMap { venueId =>
        venues.find(_.id == venueId).map(PublicVenueNoEvent.from)
      }
    },
    description = event.data.description,
    roti = event.data.roti,
    meetupUrl = event.meetupUrl
  )

  implicit val format = Json.format[PublicEventNoTalks]
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
  meetup: Option[PublicEventNoTalks]
)
object PublicTalk {
  def from(talk: Talk, speakersOpt: Option[List[Person]], eventsOpt: Option[List[Event]], venuesOpt: Option[List[Venue]]): PublicTalk = PublicTalk(
    id = talk.id,
    title = talk.data.title,
    description = talk.data.description,
    speakerIds = if (speakersOpt.isDefined) None else Some(talk.data.speakers),
    speakers = speakersOpt.map { speakers =>
      talk.data.speakers.flatMap(id => speakers.find(_.id == id)).map(speaker => PublicPerson.from(speaker, talksOpt = None, eventsOpt = None, venuesOpt = None))
    },
    slides = talk.data.slides,
    slidesEmbedCode = talk.data.slidesEmbedCode,
    video = talk.data.video,
    videoEmbedCode = talk.data.videoEmbedCode,
    meetup = eventsOpt.flatMap { events =>
      events.find(_.data.talks.contains(talk.id)).map(event => PublicEventNoTalks.from(event, venuesOpt))
    }
  )

  implicit val format = Json.format[PublicTalk]
}
