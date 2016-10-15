package com.humantalks.internal.persons

import com.humantalks.auth.silhouette.User
import com.humantalks.internal.talks.{ Talk, TalkRepository }
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

case class PersonDbService(personRepository: PersonRepository, talkRepository: TalkRepository) extends DbService[Person, Person.Id] {
  val name = personRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = personRepository.defaultSort): Future[List[Person]] = personRepository.find(filter, sort)
  def findByIds(ids: Seq[Person.Id], sort: JsObject = personRepository.defaultSort): Future[List[Person]] = personRepository.findByIds(ids, sort)
  def get(id: Person.Id): Future[Option[Person]] = personRepository.get(id)
  def create(elt: Person.Data, by: User.Id): Future[(WriteResult, Person.Id)] = personRepository.create(elt, by)
  def update(elt: Person, data: Person.Data, by: User.Id): Future[WriteResult] = personRepository.update(elt, data, by)

  def delete(id: Person.Id)(implicit ec: ExecutionContext): Future[Either[List[Talk], WriteResult]] = {
    talkRepository.findForPerson(id).flatMap { talks =>
      if (talks.isEmpty) {
        personRepository.delete(id).map(r => Right(r))
      } else {
        Future(Left(talks))
      }
    }
  }
}
