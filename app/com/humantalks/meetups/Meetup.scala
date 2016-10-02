package com.humantalks.meetups

import com.humantalks.common.models.values.Meta
import com.humantalks.persons.Person
import com.humantalks.talks.Talk
import com.humantalks.venues.Venue
import global.models.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class Meetup(
  id: Meetup.Id,
  data: Meetup.Data,
  published: Boolean,
  meta: Meta
)
object Meetup {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Meetup.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      title: String,
      date: DateTime,
      venue: Option[Venue.Id],
      talks: List[Talk.Id],
      description: Option[String],
      roti: Option[String]
  ) {
    def trim: Data = this.copy(
      title = this.title.trim,
      description = this.description.map(_.trim),
      roti = this.roti.map(_.trim)
    )
  }

  implicit val formatData = Json.format[Data]
  implicit val format = Json.format[Meetup]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Venue.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text)
  )(Meetup.Data.apply)(Meetup.Data.unapply)

  def meetupDescription(meetup: Meetup, talkList: List[Talk], personList: List[Person], venueList: List[Venue]): String = {
    def br: String = "\r\n"
    def image(url: String): String = url
    def link(name: String, url: String): String = "<a href=\"" + url + "\">" + name + "</a>"
    def bold(text: String): String = s"<b>$text</b>"
    def venueToMarkdown(venue: Venue): String =
      s"Ce mois-ci nous sommes chez ${venue.data.name}, merci à eux de nous accueillir dans leurs locaux :)$br$br" +
        venue.data.logo.map(logo => image(logo) + br + br).getOrElse("")
    def talkToMarkdown(talk: Talk, personList: List[Person]): String =
      "- " + bold(talk.data.title) + talk.data.speakers.flatMap(id => personList.find(_.id == id)).map(personToMarkdown).mkString(" par ", ", ", "") + br + br +
        talk.data.description + br + br
    def personToMarkdown(person: Person): String =
      bold(person.data.name) + person.data.twitter.map(twitter => s" (" + link("@" + twitter, "https://twitter.com/" + twitter) + ")").getOrElse("")

    val introduction = meetup.data.description.map(_ + br + br).getOrElse("")
    val venueText = meetup.data.venue.flatMap(id => venueList.find(_.id == id)).map(venueToMarkdown).getOrElse("")
    val talksText = meetup.data.talks.flatMap(id => talkList.find(_.id == id)).map(t => talkToMarkdown(t, personList)).mkString("")
    val conclusion = "Proposez vos sujets pour les prochaines sessions : " + link("http://bit.ly/HTParis-sujet", "http://bit.ly/HTParis-sujet")

    introduction + venueText + talksText + conclusion
  }
}
