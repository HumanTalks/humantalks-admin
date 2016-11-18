package global.helpers

import global.infrastructure.DbService
import play.api.mvc.Results.NotFound
import play.api.mvc._

import scala.concurrent.{ ExecutionContext, Future }

object CtrlHelper {
  def getReferer(headers: Headers): Option[String] = headers.get("Referer").orElse(headers.get("Host"))
  def getReferer(headers: Headers, default: Call): String = headers.get("Referer").orElse(headers.get("Host")).getOrElse(default.toString)

  def getQueryParam(headers: RequestHeader, name: String): Option[String] = headers.getQueryString(name)
  def getFormParam(body: AnyContent, name: String): Option[String] = body.asFormUrlEncoded.flatMap(_.get(name).flatMap(_.headOption))
  def getFormParams(body: AnyContent, name: String): Option[Seq[String]] = body.asFormUrlEncoded.flatMap(_.get(name))

  def withItem[T, Id, Data, User](srv: DbService[T, Id, Data, User])(id: Id)(block: T => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    srv.get(id).flatMap { itemOpt =>
      itemOpt.map { item =>
        block(item)
      }.getOrElse {
        Future.successful(notFound(srv.name, id))
      }
    }
  }
  def withItem[T, Id](get: Id => Future[Option[T]])(id: Id)(block: T => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    get(id).flatMap { itemOpt =>
      itemOpt.map { item =>
        block(item)
      }.getOrElse {
        Future.successful(notFound(id))
      }
    }
  }

  def notFound[Id](name: String, id: Id): Result = NotFound(global.views.html.errors.notFound("Unable to find a " + name + " with id " + id))
  def notFound[Id](id: Id): Result = NotFound(global.views.html.errors.notFound("Unable to find a " + id))
}
