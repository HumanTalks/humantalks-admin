package com.humantalks.internal.meetups

import com.humantalks.common.services.DateSrv
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.{ DateTimeConstants, LocalTime, DateTime }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Meetup(
    id: Meetup.Id,
    meetupRef: Option[Meetup.MeetupRef],
    data: Meetup.Data,
    meta: Meta
) {
  lazy val meetupUrl: Option[String] = meetupRef.map(r => s"http://www.meetup.com/fr-FR/${r.group}/events/${r.id}/")
}
object Meetup {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Meetup.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long, announced: Boolean)

  case class Data(
      title: String,
      date: DateTime,
      venue: Option[Venue.Id],
      talks: List[Talk.Id],
      description: Option[String],
      roti: Option[String],
      personCount: Option[Int]
  ) {
    def trim: Data = copy(
      title = title.trim,
      description = description.map(_.trim),
      roti = roti.map(_.trim)
    )
  }
  object Data {
    def generate(after: DateTime): Data = {
      val nextDate = DateSrv.nextDate(after, 2, DateTimeConstants.TUESDAY, new LocalTime(19, 0))
      val nextTitle = Meetup.title(nextDate)
      Meetup.Data(nextTitle, nextDate, None, List(), None, None, None)
    }
  }

  def title(date: DateTime): String = "HumanTalks Paris " + date.toString("MMMM YYYY")
  def slackChannel(date: DateTime): String = date.toString("YYYY_MM")

  implicit val formatData = Json.format[Data]
  implicit val formatRef = Json.format[MeetupRef]
  implicit val format = Json.format[Meetup]
  val fields = mapping(
    "title" -> nonEmptyText,
    "date" -> jodaDate(pattern = "dd/MM/yyyy HH:mm"),
    "venue" -> optional(of[Venue.Id]),
    "talks" -> list(of[Talk.Id]),
    "description" -> optional(text),
    "roti" -> optional(text),
    "personCount" -> optional(number)
  )(Meetup.Data.apply)(Meetup.Data.unapply)

  def meetupDescription(meetup: Meetup, venueOpt: Option[Venue], talkList: List[Talk], personList: List[Person]): String = {
    def br: String = "\r\n"
    def image(url: String): String = url
    def link(name: String, url: String): String = "<a href=\"" + url + "\">" + name + "</a>"
    def bold(text: String): String = s"<b>$text</b>"
    def venueToMarkdown(venue: Venue): String =
      s"Ce mois-ci nous sommes chez ${venue.data.name}, merci à eux de nous accueillir dans leurs locaux :)$br$br" +
        venue.data.logo.map(logo => image(logo) + br + br).getOrElse("")
    def talkToMarkdown(talk: Talk, personList: List[Person]): String =
      "- " + bold(talk.data.title) + talk.data.speakers.flatMap(id => personList.find(_.id == id)).map(personToMarkdown).mkString(" par ", ", ", "") + br + br +
        talk.data.description.map(_ + br + br).getOrElse("")
    def personToMarkdown(person: Person): String =
      bold(person.data.name) + person.data.twitter.map(twitter => s" (" + link("@" + twitter, "https://twitter.com/" + twitter) + ")").getOrElse("")

    val introduction = meetup.data.description.map(_ + br + br).getOrElse("")
    val venueText = venueOpt.map(venueToMarkdown).getOrElse("")
    val talksText = meetup.data.talks.flatMap(id => talkList.find(_.id == id)).map(t => talkToMarkdown(t, personList)).mkString("")
    val conclusion = "Proposez vos sujets pour les prochaines sessions : " + link("https://humantalksparis.herokuapp.com/submit-talk", "https://humantalksparis.herokuapp.com/submit-talk")

    introduction + venueText + talksText + conclusion
  }
}
