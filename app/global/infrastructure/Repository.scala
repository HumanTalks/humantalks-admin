package global.infrastructure

import global.values.Page
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

trait Repository[T, Id, Data, User] {
  val name: String
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]]
  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[Page[T]]
  def findByIds(ids: Seq[Id], sort: JsObject = Json.obj()): Future[List[T]]
  def get(id: Id): Future[Option[T]]
  def create(elt: Data, by: User): Future[(WriteResult, Id)]
  def update(elt: T, data: Data, by: User): Future[WriteResult]
  def delete(id: Id): Future[WriteResult]
}
object Repository {
  case class Collection[T](val value: String)
}
