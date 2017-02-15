package com.humantalks.internal.attendees

import com.humantalks.internal.events.{ Event, EventRepository }
import play.api.libs.json.JsObject
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class AttendeeDbService(attendeeRepository: AttendeeRepository) {
  def findByEvent(event: Event.Id, sort: JsObject = EventRepository.defaultSort): Future[List[Attendee]] = attendeeRepository.findByEvent(event, sort)
  def checkin(event: Event.Id, id: Long): Future[WriteResult] = attendeeRepository.checkin(event, id)
}
