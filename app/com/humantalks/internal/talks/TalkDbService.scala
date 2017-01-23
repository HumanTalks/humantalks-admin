package com.humantalks.internal.talks

import com.humantalks.exposed.proposals.{ ProposalRepository, Proposal }
import com.humantalks.internal.events.{ Event, EventRepository }
import com.humantalks.internal.persons.Person
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class TalkDbService(talkRepository: TalkRepository, eventRepository: EventRepository, proposalRepository: ProposalRepository) extends DbService[Talk, Talk.Id, Talk.Data, Person.Id] {
  val name = talkRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.find(filter, sort)
  def findPending(sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findPending(sort)
  def findByIds(ids: Seq[Talk.Id], sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findByIds(ids, sort)
  def findForPerson(id: Person.Id, sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.findForPerson(id, sort)
  def get(id: Talk.Id): Future[Option[Talk]] = talkRepository.get(id)
  def getLastAccepted: Future[Option[Talk]] = talkRepository.getLastAccepted
  def create(elt: Talk.Data, by: Person.Id): Future[(WriteResult, Talk.Id)] = talkRepository.create(elt, by)
  def update(elt: Talk, data: Talk.Data, by: Person.Id): Future[WriteResult] = talkRepository.update(elt, data, by)
  def updateAttribute(id: Talk.Id, attribute: String, value: String, by: Person.Id): Future[WriteResult] = talkRepository.updateAttribute(id, attribute, value, by)
  def setStatus(id: Talk.Id, status: Talk.Status.Value, by: Person.Id): Future[WriteResult] = talkRepository.setStatus(id, status, by)
  def delete(id: Talk.Id): Future[Either[List[Event], WriteResult]] = {
    eventRepository.findForTalk(id).flatMap { events =>
      if (events.isEmpty) {
        talkRepository.delete(id).map(r => Right(r))
      } else {
        Future.successful(Left(events))
      }
    }
  }
}
