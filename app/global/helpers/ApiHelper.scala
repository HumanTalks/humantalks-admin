package global.helpers

import global.infrastructure.DbService
import global.values.ApiError
import global.values.ApiError.Code
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.Results._
import play.api.mvc._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

object ApiHelper {

  /* Endpoint helpers */

  def find[T, Id, Data, User](srv: DbService[T, Id, Data, User])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    resultList(toEitherList(srv.find()), Ok, NotFound)
  }

  def get[T, Id, Data, User](srv: DbService[T, Id, Data, User], id: Id)(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader) = {
    result(toEither(srv.get(id)), Ok, NotFound)
  }

  def create[T, Id, Data, User](srv: DbService[T, Id, Data, User], user: User, json: JsValue)(implicit ec: ExecutionContext, r: Reads[Data], w: OWrites[T], req: RequestHeader): Future[Result] = {
    result({
      json.validate[Data] match {
        case JsSuccess(data, path) =>
          srv.create(data, user).flatMap {
            case (res, id) => returnItemOnSuccess(res, srv.get(id))
          }
        case JsError(error) =>
          Future(Left(ApiError.from(error)))
      }
    }, Created, BadRequest)
  }

  def update[T, Id, Data, User](srv: DbService[T, Id, Data, User], user: User, id: Id, json: JsValue)(implicit ec: ExecutionContext, r: Reads[Data], w: OWrites[T], req: RequestHeader): Future[Result] = {
    result({
      json.validate[Data] match {
        case JsSuccess(data, path) =>
          onSuccessFut(srv.get(id)) { elt =>
            srv.update(elt, data, user).flatMap { res =>
              returnItemOnSuccess(res, srv.get(id))
            }
          }
        case JsError(error) =>
          Future(Left(ApiError.from(error)))
      }
    }, Created, BadRequest)
  }

  def delete[T, Id, Data, User](srv: DbService[T, Id, Data, User], id: Id)(implicit ec: ExecutionContext, req: RequestHeader): Future[Result] = {
    resultJson({
      srv.delete(id).map { res =>
        onSuccess(res)(Right(JsNull))(Left(ApiError(Code.Validation, "Unable to delete element, it still has dependencies")))
      }
    }, Ok, InternalServerError)
  }

  /* Helper utils */

  private def returnItemOnSuccess[T](res: WriteResult, get: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] = {
    onSuccessFut(res) {
      onSuccess(get) { data => Right(data) }
    }
  }

  private def onSuccess[T](get: Future[Option[T]])(exec: T => Either[ApiError, T])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    get.map { dataOpt => onSuccess(dataOpt) { data => exec(data) } }
  private def onSuccessFut[T](get: Future[Option[T]])(exec: T => Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    get.flatMap { dataOpt => onSuccessFut(dataOpt) { data => exec(data) } }
  private def onSuccess[T](res: Option[T])(exec: T => Either[ApiError, T]): Either[ApiError, T] =
    res.map(exec).getOrElse(Left(ApiError.notFound()))
  private def onSuccessFut[T](res: Option[T])(exec: T => Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    res.map(exec).getOrElse(Future(Left(ApiError.notFound())))
  private def onSuccess[T](res: WriteResult)(default: Either[ApiError, T]): Either[ApiError, T] =
    ApiError.from(res).map { error => Left(error) }.getOrElse(default)
  private def onSuccessFut[T](res: WriteResult)(default: Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    ApiError.from(res).map { error => Future(Left(error)) }.getOrElse(default)
  private def onSuccess[T](res: Either[Any, WriteResult])(default: Either[ApiError, T])(objRes: Either[ApiError, T]): Either[ApiError, T] = res match {
    case Right(wr) => onSuccess(wr)(default)
    case Left(obj) => objRes
  }

  private def toEither[T](e: Future[Option[T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] = e.map(_.map(l => Right(l)).getOrElse(Left(ApiError.notFound())))
  private def toEitherList[T](e: Future[List[T]])(implicit ec: ExecutionContext): Future[Either[ApiError, List[T]]] = e.map(l => Right(l))

  /* Api results */

  def result[T](exec: Future[Either[ApiError, T]], success: Status, error: Status)(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    resultJson(exec.map(e => e.right.map(d => Json.toJson(d))), success, error)
  }
  def resultList[T](exec: Future[Either[ApiError, List[T]]], success: Status = Ok, error: Status = NotFound)(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    resultJson(exec.map(e => e.right.map(d => Json.toJson(d))), success, error)
  }
  def resultJson(exec: Future[Either[ApiError, JsValue]], success: Status, error: Status)(implicit ec: ExecutionContext, req: RequestHeader): Future[Result] = {
    val start = new DateTime()
    exec.map {
      _ match {
        case Right(data) => writeSuccess(success, start, data)
        case Left(err) => writeError(error, start, err)
      }
    }.recover {
      case e: Throwable => writeError(InternalServerError, start, ApiError.from(e))
    }
  }
  private def writeSuccess(status: Status, start: DateTime, data: JsValue)(implicit req: RequestHeader): Result =
    write(status, Json.obj(
      "data" -> data,
      "metas" -> metas(start)
    ))
  private def writeError(status: Status, start: DateTime, error: ApiError)(implicit req: RequestHeader): Result =
    write(status, Json.obj(
      "error" -> error,
      "metas" -> metas(start)
    ))
  private def metas(start: DateTime): JsObject = Json.obj(
    "exec" -> (new DateTime().getMillis - start.getMillis)
  )
  private def write(status: Status, data: JsObject)(implicit req: RequestHeader): Result =
    req.queryString.get("pretty").flatMap(_.find(_ == "false")).map { _ =>
      status(data).as("application/json;charset=utf-8")
    }.getOrElse {
      status(Json.prettyPrint(data)).as("application/json;charset=utf-8") // pretty print to be more developper friendly
    }
}
