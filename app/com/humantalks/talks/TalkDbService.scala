package com.humantalks.talks

import com.humantalks.auth.models.User
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class TalkDbService(talkRepository: TalkRepository) extends DbService[Talk, Talk.Id] {
  val name = talkRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = talkRepository.defaultSort): Future[List[Talk]] = talkRepository.find(filter, sort)
  def get(id: Talk.Id): Future[Option[Talk]] = talkRepository.get(id)
  def create(elt: Talk.Data, by: User.Id): Future[(WriteResult, Talk.Id)] = talkRepository.create(elt, by)
  def update(elt: Talk, data: Talk.Data, by: User.Id): Future[WriteResult] = talkRepository.update(elt, data, by)
}
