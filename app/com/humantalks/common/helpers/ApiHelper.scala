package com.humantalks.common.helpers

import com.humantalks.common.infrastructure.Repository
import com.humantalks.common.models.User
import global.models.{ ApiError, Page }
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ Future, ExecutionContext }

object ApiHelper {

  /* Play actions */

  def findAction[T, Id, TData](repo: Repository[T, Id, TData])(implicit ec: ExecutionContext, w: OWrites[T]) = Action.async { implicit req: Request[AnyContent] =>
    find() {
      repo.find()
    }
  }

  def getAction[T, Id, TData](repo: Repository[T, Id, TData])(id: Id)(implicit ec: ExecutionContext, w: OWrites[T]) = Action.async { implicit req: Request[AnyContent] =>
    get() {
      repo.get(id)
    }
  }

  def createAction[T, Id, TData](repo: Repository[T, Id, TData])(implicit ec: ExecutionContext, rd: Reads[TData], wd: OWrites[TData], w: OWrites[T]) = Action.async(parse.json) { implicit req: Request[JsValue] =>
    create()(
      req.body.validate[TData],
      (data: TData) => repo.create(data, User.fake),
      repo.get
    )
  }

  def updateAction[T, Id, TData](repo: Repository[T, Id, TData])(id: Id)(implicit ec: ExecutionContext, r: Reads[TData], w: OWrites[T]) = Action.async(parse.json) { implicit req: Request[JsValue] =>
    update(id)(
      req.body.validate[TData],
      (elt: T, data: TData) => repo.update(elt, data, User.fake),
      repo.get
    )
  }

  def deleteAction[T, Id, TData](repo: Repository[T, Id, TData])(id: Id)(implicit ec: ExecutionContext) = Action.async { implicit req: Request[AnyContent] =>
    delete() {
      repo.delete(id)
    }
  }

  /* Action helpers */

  def find[T]()(find: => Future[List[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    val start = new DateTime()
    find.map {
      list => resultListSuccess(Ok, start, list)
    }.recover {
      case error: Throwable => resultError(InternalServerError, start, ApiError.from(error))
    }
  }

  def get[T]()(get: => Future[Option[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader) = {
    resultItemSuccess(Ok, new DateTime(), get)
  }

  def create[T, Id, TData]()(
    validation: JsResult[TData],
    create: TData => Future[(WriteResult, Id)],
    get: Id => Future[Option[T]]
  )(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    val start = new DateTime()
    validation match {
      case JsSuccess(data, path) =>
        create(data).flatMap {
          case (res, id) => resultItem(Created, res, start, get(id))
        }.recover {
          case error: Throwable => resultError(InternalServerError, start, ApiError.from(error))
        }
      case JsError(error) =>
        Future(resultError(BadRequest, start, ApiError.from(error)))
    }
  }

  def update[T, Id, TData](id: Id)(
    validation: JsResult[TData],
    update: (T, TData) => Future[WriteResult],
    get: Id => Future[Option[T]]
  )(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    val start = new DateTime()
    validation match {
      case JsSuccess(data, path) =>
        get(id).flatMap { eltOpt =>
          eltOpt.map { elt =>
            update(elt, data).flatMap { res =>
              resultItem(Ok, res, start, get(id))
            }
          }.getOrElse {
            Future(resultError(NotFound, start, ApiError.notFound()))
          }
        }.recover {
          case error: Throwable => resultError(InternalServerError, start, ApiError.from(error))
        }
      case JsError(error) =>
        Future(resultError(BadRequest, start, ApiError.from(error)))
    }
  }

  def delete()(delete: => Future[WriteResult])(implicit ec: ExecutionContext, req: RequestHeader): Future[Result] = {
    val start = new DateTime()
    delete.map { res =>
      ApiError.from(res).map { error =>
        resultError(InternalServerError, start, error)
      }.getOrElse {
        NoContent
      }
    }.recover {
      case error: Throwable => resultError(InternalServerError, start, ApiError.from(error))
    }
  }

  /* Helper utils */

  private def resultListSuccess[T](status: Status, start: DateTime, list: List[T])(implicit w: OWrites[T], req: RequestHeader): Result =
    writeResult(status, Json.obj(
      "data" -> list,
      "metas" -> metas(start)
    ))
  private def resultItem[T](successStatus: Status, res: WriteResult, start: DateTime, get: => Future[Option[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] =
    ApiError.from(res).map { error =>
      Future(resultError(InternalServerError, start, error))
    }.getOrElse {
      resultItemSuccess(successStatus, start, get)
    }
  private def resultItemSuccess[T](successStatus: Status, start: DateTime, get: => Future[Option[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader) =
    get.map { dataOpt =>
      dataOpt.map { data =>
        resultSuccess(successStatus, start, data)
      }.getOrElse {
        resultError(NotFound, start, ApiError.notFound())
      }
    }.recover {
      case error: Throwable => resultError(InternalServerError, start, ApiError.from(error))
    }
  private def resultSuccess[T](status: Status, start: DateTime, data: T)(implicit w: OWrites[T], req: RequestHeader): Result =
    writeResult(status, Json.obj(
      "data" -> data,
      "metas" -> metas(start)
    ))
  private def resultError(status: Status, start: DateTime, error: ApiError)(implicit req: RequestHeader): Result =
    writeResult(status, Json.obj(
      "error" -> error,
      "metas" -> metas(start)
    ))
  private def writeResult(status: Status, data: JsObject)(implicit req: RequestHeader): Result =
    req.queryString.get("pretty").flatMap(_.find(_ == "false")).map { _ =>
      status(data).as("application/json;charset=utf-8")
    }.getOrElse {
      status(Json.prettyPrint(data)).as("application/json;charset=utf-8") // pretty print to be more developper friendly
    }
  private def metas(start: DateTime): JsObject = Json.obj(
    "exec" -> (new DateTime().getMillis - start.getMillis)
  )
}
