package com.humantalks.exposed.proposals

import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.TalkRepository
import global.infrastructure.DbService
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class ProposalDbService(talkRepository: TalkRepository, proposalRepository: ProposalRepository) extends DbService[Proposal, Proposal.Id, Proposal.Data, Person.Id] {
  val name = proposalRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.find(filter, sort)
  def findByIds(ids: Seq[Proposal.Id], sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.findByIds(ids, sort)
  def findForPerson(id: Person.Id, sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.findForPerson(id, sort)
  def get(id: Proposal.Id): Future[Option[Proposal]] = proposalRepository.get(id)
  def create(elt: Proposal.Data, by: Person.Id): Future[(WriteResult, Proposal.Id)] = proposalRepository.create(elt, by)
  def update(elt: Proposal, data: Proposal.Data, by: Person.Id): Future[WriteResult] =
    proposalRepository.update(elt, data, by).map { res =>
      for {
        proposalOpt <- proposalRepository.get(elt.id)
        talkOpt <- proposalOpt.flatMap(_.talk).map(talkRepository.get).getOrElse(Future.successful(None))
      } yield talkOpt.map { talk =>
        talkRepository.update(talk, data.toTalk, by)
      }
      res
    }
  def delete(id: Proposal.Id): Future[Either[Nothing, WriteResult]] = proposalRepository.delete(id).map(r => Right(r))
}
