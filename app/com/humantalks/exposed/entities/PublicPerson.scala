package com.humantalks.exposed.entities

import com.humantalks.internal.meetups.Meetup
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
  meetup: Option[PublicMeetupNoTalks]
)
object PublicTalkNoSpeakers {
  def from(talk: Talk, meetupsOpt: Option[List[Meetup]], venuesOpt: Option[List[Venue]]): PublicTalkNoSpeakers = PublicTalkNoSpeakers(
    id = talk.id,
    title = talk.data.title,
    description = talk.data.description,
    speakerIds = talk.data.speakers,
    slides = talk.data.slides,
    slidesEmbedCode = talk.data.slidesEmbedCode,
    video = talk.data.video,
    videoEmbedCode = talk.data.videoEmbedCode,
    meetup = meetupsOpt.flatMap { meetups =>
      meetups.find(_.data.talks.contains(talk.id)).map(meetup => PublicMeetupNoTalks.from(meetup, venuesOpt))
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
  def from(person: Person, talksOpt: Option[List[Talk]], meetupsOpt: Option[List[Meetup]], venuesOpt: Option[List[Venue]]): PublicPerson = PublicPerson(
    id = person.id,
    name = person.data.name,
    twitter = person.data.twitter,
    avatar = person.data.avatar,
    description = person.data.description,
    talks = talksOpt.map { talks =>
      talks.filter(talk => talk.data.speakers.contains(person.id)).map(talk => PublicTalkNoSpeakers.from(talk, meetupsOpt, venuesOpt))
    }
  )

  implicit val format = Json.format[PublicPerson]
}
