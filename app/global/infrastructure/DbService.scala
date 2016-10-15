package global.infrastructure

import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

trait DbService[T, Id, Data, User] {
  val name: String
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]]
  def get(id: Id): Future[Option[T]]
  def create(data: Data, by: User): Future[(WriteResult, Id)]
  def update(elt: T, data: Data, by: User): Future[WriteResult]
  def delete(id: Id)(implicit ec: ExecutionContext): Future[Either[Any, WriteResult]]
}
