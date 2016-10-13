package global.infrastructure

import play.api.libs.json.{ Json, JsObject }

import scala.concurrent.Future

trait DbService[T, Id] {
  val name: String
  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj()): Future[List[T]]
  def get(id: Id): Future[Option[T]]
}
