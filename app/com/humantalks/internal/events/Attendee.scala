package com.humantalks.internal.events

import com.humantalks.common.services.meetup.models.MeetupRsvp
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Attendee(
  id: Long,
  name: String,
  avatar: Option[String],
  event: Event.Id,
  group: String,
  meetup: String,
  status: String,
  created: DateTime,
  updated: DateTime
)
object Attendee {
  def from(eventId: Event.Id, rsvp: MeetupRsvp): Attendee = Attendee(
    id = rsvp.member.id,
    name = rsvp.member.name,
    avatar = rsvp.member.photo.map(_.photo_link),
    event = eventId,
    group = rsvp.group.urlname,
    meetup = rsvp.event.id,
    status = rsvp.response,
    created = rsvp.created,
    updated = rsvp.updated
  )

  private val separator = ";"
  private val lineSeparator = "\n"
  private val headers = List("nom", "status", "inscription").mkString(separator)
  private def row(attendee: Attendee): String = List(
    attendee.name,
    attendee.status,
    attendee.created.toString("dd/MM/YYYY HH:mm")
  ).mkString(separator)
  def toCsv(attendees: List[Attendee]): String = {
    val attendeesByStatus = attendees.sortBy(_.name).groupBy(_.status)
    (
      List(headers) ++
      attendeesByStatus.getOrElse("yes", List()).map(row) ++
      attendeesByStatus.getOrElse("waitlist", List()).map(row)
    ).mkString(lineSeparator)
  }

  implicit val format = Json.format[Attendee]
}