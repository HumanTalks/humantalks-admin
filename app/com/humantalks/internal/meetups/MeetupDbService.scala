package com.humantalks.internal.meetups

import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class MeetupDbService(meetupRepository: MeetupRepository) extends DbService[Meetup, Meetup.Id, Meetup.Data, Person.Id] {
  val name = meetupRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.find(filter, sort)
  def findForTalk(id: Talk.Id, sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findForTalk(id, sort)
  def findForVenue(id: Venue.Id, sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findForVenue(id, sort)
  def get(id: Meetup.Id): Future[Option[Meetup]] = meetupRepository.get(id)
  def create(data: Meetup.Data, by: Person.Id): Future[(WriteResult, Meetup.Id)] = meetupRepository.create(data, by)
  def update(elt: Meetup, data: Meetup.Data, by: Person.Id): Future[WriteResult] = meetupRepository.update(elt, data, by)
  def addTalk(id: Meetup.Id, talkId: Talk.Id): Future[WriteResult] = meetupRepository.addTalk(id, talkId)
  def removeTalk(id: Meetup.Id, talkId: Talk.Id): Future[WriteResult] = meetupRepository.removeTalk(id, talkId)
  def setPublished(id: Meetup.Id): Future[WriteResult] = meetupRepository.setPublished(id)
  def delete(id: Meetup.Id): Future[Either[Nothing, WriteResult]] = meetupRepository.delete(id).map(res => Right(res))
}
