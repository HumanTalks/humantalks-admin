package com.humantalks.internal.talks

import com.humantalks.exposed.proposals.{ ProposalRepository, Proposal }
import com.humantalks.internal.meetups.{ Meetup, MeetupRepository }
import com.humantalks.internal.persons.Person
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class TalkDbService(talkRepository: TalkRepository, meetupRepository: MeetupRepository, proposalRepository: ProposalRepository) extends DbService[Talk, Talk.Id, Talk.Data, Person.Id] {
  val name = talkRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.find(filter, sort)
  def findPending(sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findPending(sort)
  def findByIds(ids: Seq[Talk.Id], sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findByIds(ids, sort)
  def findForPerson(id: Person.Id, sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findForPerson(id, sort)
  def get(id: Talk.Id): Future[Option[Talk]] = talkRepository.get(id)
  def create(elt: Talk.Data, by: Person.Id): Future[(WriteResult, Talk.Id)] = talkRepository.create(elt, by)
  def create(proposal: Proposal, by: Person.Id): Future[Either[String, Talk.Id]] = {
    proposal.talk.map { talkId =>
      Future.successful(Left("This proposal already has a talk !"))
    }.getOrElse {
      talkRepository.create(proposal.data, by).flatMap {
        case (res, talkId) =>
          if (res.ok) {
            proposalRepository.setTalk(proposal.id, talkId, by).map(r => Right(talkId))
          } else {
            Future.successful(Left("Unable to create talk !"))
          }
      }
    }
  }
  def update(elt: Talk, data: Talk.Data, by: Person.Id): Future[WriteResult] = talkRepository.update(elt, data, by)
  def accept(id: Talk.Id, by: Person.Id): Future[WriteResult] = talkRepository.setStatus(id, Talk.Status.Accepted, by)
  def reject(id: Talk.Id, by: Person.Id): Future[WriteResult] = talkRepository.setStatus(id, Talk.Status.Rejected, by)

  def delete(id: Talk.Id): Future[Either[List[Meetup], WriteResult]] = {
    meetupRepository.findForTalk(id).flatMap { meetups =>
      if (meetups.isEmpty) {
        talkRepository.delete(id).map(r => Right(r))
      } else {
        Future.successful(Left(meetups))
      }
    }
  }
}
