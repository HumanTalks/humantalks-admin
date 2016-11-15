package com.humantalks.internal.admin.config

import com.humantalks.internal.persons.Person
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class ConfigDbService(configRepository: ConfigRepository) extends DbService[Config, Config.Id, Config.Data, Person.Id] {
  val name = configRepository.name
  def find(filter: JsObject = Json.obj(), sort: JsObject = configRepository.defaultSort): Future[List[Config]] = configRepository.find(filter, sort)
  def get(id: Config.Id): Future[Option[Config]] = configRepository.get(id)
  def create(elt: Config.Data, by: Person.Id): Future[(WriteResult, Config.Id)] = configRepository.create(elt, by)
  def update(elt: Config, data: Config.Data, by: Person.Id): Future[WriteResult] = configRepository.update(elt, data, by)
  def setValue(id: Config.Id, value: String, by: Person.Id): Future[WriteResult] = configRepository.setValue(id, value, by)
  def delete(id: Config.Id): Future[Either[Nothing, WriteResult]] = configRepository.delete(id).map(res => Right(res))
}
