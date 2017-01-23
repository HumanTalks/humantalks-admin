package com.humantalks.internal.events

import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.{ TalkRepository, Talk }
import com.humantalks.internal.venues.Venue
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class EventDbService(talkRepository: TalkRepository, eventRepository: EventRepository) extends DbService[Event, Event.Id, Event.Data, Person.Id] {
  val name = eventRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.find(filter, sort)
  def findPast(sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.findPast(sort)
  def findByIds(ids: Seq[Event.Id], sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.findByIds(ids, sort)
  def findForTalk(id: Talk.Id, sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.findForTalk(id, sort)
  def findForTalks(ids: Seq[Talk.Id], sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.findForTalks(ids, sort)
  def findForVenue(id: Venue.Id, sort: JsObject = eventRepository.defaultSort): Future[List[Event]] = eventRepository.findForVenue(id, sort)
  def get(id: Event.Id): Future[Option[Event]] = eventRepository.get(id)
  def getLast: Future[Option[Event]] = eventRepository.getLast
  def getNext: Future[Option[Event]] = eventRepository.getNext
  def create(data: Event.Data, by: Person.Id): Future[(WriteResult, Event.Id)] = eventRepository.create(data, by)
  def update(elt: Event, data: Event.Data, by: Person.Id): Future[WriteResult] = eventRepository.update(elt, data, by)
  def setVenue(id: Event.Id, venueId: Venue.Id, by: Person.Id): Future[WriteResult] = eventRepository.setVenue(id, venueId, by)
  def addTalk(id: Event.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    eventRepository.addTalk(id, talkId, by).map { res =>
      talkRepository.get(talkId).map {
        _.map { talk => talkRepository.setStatus(talkId, Talk.Status.Planified, by) }
      }
      res
    }
  def moveTalk(id: Event.Id, talkId: Talk.Id, up: Boolean, by: Person.Id): Future[Option[WriteResult]] = eventRepository.moveTalk(id, talkId, up, by)
  def removeTalk(id: Event.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    eventRepository.removeTalk(id, talkId, by).map { res =>
      talkRepository.get(talkId).map {
        _.map { talk =>
          talkRepository.setStatus(talkId, Talk.Status.Accepted, by)
        }
      }
      res
    }
  def setMeetupRef(id: Event.Id, meetupRef: Event.MeetupRef, by: Person.Id): Future[WriteResult] = eventRepository.setMeetupRef(id, meetupRef, by)
  def unsetMeetupRef(id: Event.Id, by: Person.Id): Future[WriteResult] = eventRepository.unsetMeetupRef(id, by)
  def delete(id: Event.Id): Future[Either[Nothing, WriteResult]] = eventRepository.delete(id).map(res => Right(res))
}
