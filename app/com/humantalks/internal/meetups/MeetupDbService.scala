package com.humantalks.internal.meetups

import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.{ TalkRepository, Talk }
import com.humantalks.internal.venues.Venue
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class MeetupDbService(talkRepository: TalkRepository, meetupRepository: MeetupRepository) extends DbService[Meetup, Meetup.Id, Meetup.Data, Person.Id] {
  val name = meetupRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.find(filter, sort)
  def findPublished(sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.find(Json.obj("published" -> true), sort)
  def findByIds(ids: Seq[Meetup.Id], sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findByIds(ids, sort)
  def findForTalk(id: Talk.Id, sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findForTalk(id, sort)
  def findForTalks(ids: Seq[Talk.Id], sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findForTalks(ids, sort)
  def findForVenue(id: Venue.Id, sort: JsObject = meetupRepository.defaultSort): Future[List[Meetup]] = meetupRepository.findForVenue(id, sort)
  def get(id: Meetup.Id): Future[Option[Meetup]] = meetupRepository.get(id)
  def create(data: Meetup.Data, by: Person.Id): Future[(WriteResult, Meetup.Id)] = meetupRepository.create(data, by)
  def update(elt: Meetup, data: Meetup.Data, by: Person.Id): Future[WriteResult] = meetupRepository.update(elt, data, by)
  def addTalk(id: Meetup.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] = meetupRepository.addTalk(id, talkId, by)
  def moveTalk(id: Meetup.Id, talkId: Talk.Id, up: Boolean, by: Person.Id): Future[Option[WriteResult]] = meetupRepository.moveTalk(id, talkId, up, by)
  def removeTalk(id: Meetup.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    meetupRepository.removeTalk(id, talkId, by).map { res =>
      talkRepository.get(talkId).map {
        _.map { talk =>
          if (talk.status == Talk.Status.Accepted) {
            talkRepository.setStatus(talkId, Talk.Status.Proposed, by)
          }
        }
      }
      res
    }
  def setPublished(id: Meetup.Id, by: Person.Id): Future[WriteResult] = meetupRepository.setPublished(id, by)
  def delete(id: Meetup.Id): Future[Either[Nothing, WriteResult]] = meetupRepository.delete(id).map(res => Right(res))
}
