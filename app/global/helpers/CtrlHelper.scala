package global.helpers

import global.infrastructure.{ DbService, Repository }
import play.api.mvc.Results.NotFound
import play.api.mvc.{ Action, AnyContent, Request, Result }

import scala.concurrent.{ ExecutionContext, Future }

object CtrlHelper {

  def findAction[T, Id, Data, User](srv: DbService[T, Id, Data, User])(html: List[T] => Future[Result])(implicit ec: ExecutionContext) = Action.async { implicit req: Request[AnyContent] =>
    srv.find().flatMap { list =>
      html(list)
    }
  }

  // Service[T, Id]  to  {name: String, get: Id => Future[Option[T]]}
  def withItem[T, Id, Data, User](srv: DbService[T, Id, Data, User])(id: Id)(block: T => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    srv.get(id).flatMap { itemOpt =>
      itemOpt.map { item =>
        block(item)
      }.getOrElse {
        Future(notFound(srv.name, id))
      }
    }
  }
  def notFound[Id](name: String, id: Id): Result =
    NotFound(global.views.html.errors.notFound("Unable to find a " + name + " with id " + id))
}
