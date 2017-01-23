package com.humantalks.exposed.entities

import com.humantalks.internal.events.Event
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import play.api.libs.json.Json

case class PublicTalkNoSpeakers(
  id: Talk.Id,
  title: String,
  description: Option[String],
  speakerIds: List[Person.Id],
  slides: Option[String],
  slidesEmbedCode: Option[String],
  video: Option[String],
  videoEmbedCode: Option[String],
  meetup: Option[PublicEventNoTalks]
)
object PublicTalkNoSpeakers {
  def from(talk: Talk, eventsOpt: Option[List[Event]], venuesOpt: Option[List[Venue]]): PublicTalkNoSpeakers = PublicTalkNoSpeakers(
    id = talk.id,
    title = talk.data.title,
    description = talk.data.description,
    speakerIds = talk.data.speakers,
    slides = talk.data.slides,
    slidesEmbedCode = talk.data.slidesEmbedCode,
    video = talk.data.video,
    videoEmbedCode = talk.data.videoEmbedCode,
    meetup = eventsOpt.flatMap { events =>
      events.find(_.data.talks.contains(talk.id)).map(event => PublicEventNoTalks.from(event, venuesOpt))
    }
  )

  implicit val format = Json.format[PublicTalkNoSpeakers]
}

case class PublicPerson(
  id: Person.Id,
  name: String,
  twitter: Option[String],
  avatar: Option[String],
  description: Option[String],
  talks: Option[List[PublicTalkNoSpeakers]]
)
object PublicPerson {
  def from(person: Person, talksOpt: Option[List[Talk]], eventsOpt: Option[List[Event]], venuesOpt: Option[List[Venue]]): PublicPerson = PublicPerson(
    id = person.id,
    name = person.data.name,
    twitter = person.data.twitter,
    avatar = person.data.avatar,
    description = person.data.description,
    talks = talksOpt.map { talks =>
      talks.filter(talk => talk.data.speakers.contains(person.id)).map(talk => PublicTalkNoSpeakers.from(talk, eventsOpt, venuesOpt))
    }
  )

  implicit val format = Json.format[PublicPerson]
}
