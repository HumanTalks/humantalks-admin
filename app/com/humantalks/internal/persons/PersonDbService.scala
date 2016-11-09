package com.humantalks.internal.persons

import com.humantalks.exposed.proposals.{ Proposal, ProposalRepository }
import com.humantalks.internal.talks.{ Talk, TalkRepository }
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class PersonDbService(personRepository: PersonRepository, talkRepository: TalkRepository, proposalRepository: ProposalRepository) extends DbService[Person, Person.Id, Person.Data, Person.Id] {
  val name = personRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = personRepository.defaultSort): Future[List[Person]] = personRepository.find(filter, sort)
  def findUsers(filter: JsObject = Json.obj(), sort: JsObject = personRepository.defaultSort): Future[List[Person]] = personRepository.findUsers(filter, sort)
  def findByIds(ids: Seq[Person.Id], sort: JsObject = personRepository.defaultSort): Future[List[Person]] = personRepository.findByIds(ids, sort)
  def get(id: Person.Id): Future[Option[Person]] = personRepository.get(id)
  def create(elt: Person.Data, by: Person.Id): Future[(WriteResult, Person.Id)] =
    personRepository.getByEmail(elt.email.get).flatMap { personOpt =>
      personOpt.map { person =>
        Future.failed(new IllegalArgumentException("A person with email " + elt.email.get + " already exists"))
      }.getOrElse {
        personRepository.create(elt, by)
      }
    }
  def update(elt: Person, data: Person.Data, by: Person.Id): Future[WriteResult] = {
    data.email.map { email =>
      personRepository.getByEmail(email).flatMap { personOpt =>
        personOpt.filter(_.id != elt.id).map { person =>
          Future.failed(new IllegalArgumentException("A person with email " + email + " already exists"))
        }.getOrElse {
          personRepository.update(elt, data, by)
        }
      }
    }.getOrElse {
      personRepository.update(elt, data, by)
    }
  }
  def setRole(id: Person.Id, role: Option[Person.Role.Value], by: Person.Id): Future[WriteResult] = personRepository.setRole(id, role, by)

  def delete(id: Person.Id): Future[Either[(List[Talk], List[Proposal]), WriteResult]] = {
    (for {
      talks <- talkRepository.findForPerson(id)
      proposals <- proposalRepository.findForPerson(id)
    } yield (talks, proposals)).flatMap {
      case (talks, proposals) =>
        if (talks.isEmpty && proposals.isEmpty) {
          personRepository.delete(id).map(r => Right(r))
        } else {
          Future.successful(Left((talks, proposals)))
        }
    }
  }
}
