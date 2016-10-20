package com.humantalks.exposed.proposals

import com.humantalks.internal.persons.Person
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

case class ProposalDbService(proposalRepository: ProposalRepository) {
  val name = proposalRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.find(filter, sort)
  def findByIds(ids: Seq[Proposal.Id], sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.findByIds(ids, sort)
  def findForPerson(id: Person.Id, sort: JsObject = proposalRepository.defaultSort): Future[List[Proposal]] = proposalRepository.findForPerson(id, sort)
  def get(id: Proposal.Id): Future[Option[Proposal]] = proposalRepository.get(id)
  def create(elt: Proposal.Data): Future[(WriteResult, Proposal)] = proposalRepository.create(elt)
  def update(elt: Proposal, data: Proposal.Data): Future[WriteResult] = proposalRepository.update(elt, data)
  def delete(id: Proposal.Id)(implicit ec: ExecutionContext): Future[Either[Nothing, WriteResult]] = proposalRepository.delete(id).map(r => Right(r))
}
