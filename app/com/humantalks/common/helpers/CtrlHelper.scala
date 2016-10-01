package com.humantalks.common.helpers

import com.humantalks.common.infrastructure.Repository
import play.api.libs.json.OWrites
import play.api.mvc.{ AnyContent, Request, Action, Result }
import play.api.mvc.Results.NotFound

import scala.concurrent.{ ExecutionContext, Future }

object CtrlHelper {

  def findAction[T, Id, TData](repo: Repository[T, Id, TData])(html: List[T] => Future[Result])(implicit ec: ExecutionContext) = Action.async { implicit req: Request[AnyContent] =>
    repo.find().flatMap { list =>
      html(list)
    }
  }

  def withItem[T, Id, TData](repo: Repository[T, Id, TData])(id: Id)(block: T => Future[Result])(implicit ec: ExecutionContext): Future[Result] = {
    repo.get(id).flatMap { itemOpt =>
      itemOpt.map { item =>
        block(item)
      }.getOrElse {
        Future(notFound(repo.name, id))
      }
    }
  }
  def notFound[Id](name: String, id: Id): Result =
    NotFound(com.humantalks.common.views.html.errors.notFound("Unable to find a " + name + " with id " + id))
}
