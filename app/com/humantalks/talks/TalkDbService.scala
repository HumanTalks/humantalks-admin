package com.humantalks.talks

import com.humantalks.auth.models.User
import com.humantalks.meetups.{ Meetup, MeetupRepository }
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

case class TalkDbService(meetupRepository: MeetupRepository, talkRepository: TalkRepository) extends DbService[Talk, Talk.Id] {
  val name = talkRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.find(filter, sort)
  def get(id: Talk.Id): Future[Option[Talk]] = talkRepository.get(id)
  def create(elt: Talk.Data, by: User.Id): Future[(WriteResult, Talk.Id)] = talkRepository.create(elt, by)
  def update(elt: Talk, data: Talk.Data, by: User.Id): Future[WriteResult] = talkRepository.update(elt, data, by)

  def delete(id: Talk.Id)(implicit ec: ExecutionContext): Future[Either[List[Meetup], WriteResult]] = {
    meetupRepository.findForTalk(id).flatMap { meetups =>
      if (meetups.isEmpty) {
        talkRepository.delete(id).map(r => Right(r))
      } else {
        Future(Left(meetups))
      }
    }
  }
}
