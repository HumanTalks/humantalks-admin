package com.humantalks.exposed.entities

import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.Partner
import org.joda.time.DateTime
import play.api.libs.json.Json

case class PublicEventNoTalks(
  id: Event.Id,
  title: String,
  date: DateTime,
  venueId: Option[Partner.Id],
  venue: Option[PublicPartnerNoEvent],
  description: Option[String],
  roti: Option[String],
  meetupUrl: Option[String]
)
object PublicEventNoTalks {
  def from(event: Event, partnersOpt: Option[List[Partner]]): PublicEventNoTalks = PublicEventNoTalks(
    id = event.id,
    title = event.data.title,
    date = event.data.date,
    venueId = if (partnersOpt.isDefined) None else event.data.venue,
    venue = partnersOpt.flatMap { partners =>
      event.data.venue.flatMap { venueId =>
        partners.find(_.id == venueId).map(PublicPartnerNoEvent.from)
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
  def from(talk: Talk, speakersOpt: Option[List[Person]], eventsOpt: Option[List[Event]], partnersOpt: Option[List[Partner]]): PublicTalk = PublicTalk(
    id = talk.id,
    title = talk.data.title,
    description = talk.data.description,
    speakerIds = if (speakersOpt.isDefined) None else Some(talk.data.speakers),
    speakers = speakersOpt.map { speakers =>
      talk.data.speakers.flatMap(id => speakers.find(_.id == id)).map(speaker => PublicPerson.from(speaker, talksOpt = None, eventsOpt = None, partnersOpt = None))
    },
    slides = talk.data.slides,
    slidesEmbedCode = talk.data.slidesEmbedCode,
    video = talk.data.video,
    videoEmbedCode = talk.data.videoEmbedCode,
    meetup = eventsOpt.flatMap { events =>
      events.find(_.data.talks.contains(talk.id)).map(event => PublicEventNoTalks.from(event, partnersOpt))
    }
  )

  implicit val format = Json.format[PublicTalk]
}
