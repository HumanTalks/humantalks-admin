package global.infrastructure

import global.models.Page
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

trait GenericRepository[T, Id, TData] {
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]]
  def findByIds(ids: Seq[Id], sort: JsObject = Json.obj()): Future[List[T]]
  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[Page[T]]
  def get(id: Id): Future[Option[T]]
  def create(elt: TData): Future[(WriteResult, Id)]
  def fullUpdate(id: Id, elt: T): Future[WriteResult]
  def update(id: Id, patch: JsObject): Future[WriteResult]
  def delete(id: Id): Future[WriteResult]
}
object GenericRepository {
  case class Collection[T](val value: String)
}
