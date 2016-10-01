package com.humantalks.common.infrastructure

import com.humantalks.common.models.User
import global.models.Page
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

trait Repository[T, Id, TData] {
  val name: String
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]]
  def findByIds(ids: Seq[Id], sort: JsObject = Json.obj()): Future[List[T]]
  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[Page[T]]
  def get(id: Id): Future[Option[T]]
  def create(elt: TData, by: User.Id): Future[(WriteResult, Id)]
  def update(elt: T, data: TData, by: User.Id): Future[WriteResult]
  def partialUpdate(id: Id, patch: JsObject): Future[WriteResult]
  def delete(id: Id): Future[WriteResult]
}
object Repository {
  case class Collection[T](val value: String)
}
