package global.helpers

import com.humantalks.auth.models.User
import global.infrastructure.Repository
import global.models.ApiError
import org.joda.time.DateTime
import play.api.libs.json._
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Results._
import play.api.mvc._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

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

  /* Endpoint helpers */

  def find[T]()(find: => Future[List[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    resultList(toEitherList(find), Ok, NotFound)
  }

  def get[T]()(get: => Future[Option[T]])(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader) = {
    result(toEither(get), Ok, NotFound)
  }

  def create[T, Id, TData]()(
    validation: JsResult[TData],
    create: TData => Future[(WriteResult, Id)],
    get: Id => Future[Option[T]]
  )(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    result({
      validation match {
        case JsSuccess(data, path) =>
          create(data).flatMap {
            case (res, id) => returnItemOnSuccess(res, get(id))
          }
        case JsError(error) =>
          Future(Left(ApiError.from(error)))
      }
    }, Created, BadRequest)
  }

  def update[T, Id, TData](id: Id)(
    validation: JsResult[TData],
    update: (T, TData) => Future[WriteResult],
    get: Id => Future[Option[T]]
  )(implicit ec: ExecutionContext, w: OWrites[T], req: RequestHeader): Future[Result] = {
    result({
      validation match {
        case JsSuccess(data, path) =>
          onSuccessFut(get(id)) { elt =>
            update(elt, data).flatMap { res =>
              returnItemOnSuccess(res, get(id))
            }
          }
        case JsError(error) =>
          Future(Left(ApiError.from(error)))
      }
    }, Created, BadRequest)
  }

  def delete()(delete: => Future[WriteResult])(implicit ec: ExecutionContext, req: RequestHeader): Future[Result] = {
    resultJson({
      delete.map { res =>
        onSuccess(res)(Right(JsNull))
      }
    }, Ok, InternalServerError)
  }

  /* Helper utils */

  def returnItemOnSuccess[T](res: WriteResult, get: => Future[Option[T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] = {
    onSuccessFut(res) {
      onSuccess(get) { data => Right(data) }
    }
  }

  def onSuccess[T](get: Future[Option[T]])(exec: T => Either[ApiError, T])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    get.map { dataOpt => onSuccess(dataOpt) { data => exec(data) } }
  def onSuccessFut[T](get: Future[Option[T]])(exec: T => Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    get.flatMap { dataOpt => onSuccessFut(dataOpt) { data => exec(data) } }
  def onSuccess[T](res: Option[T])(exec: T => Either[ApiError, T]): Either[ApiError, T] =
    res.map(exec).getOrElse(Left(ApiError.notFound()))
  def onSuccessFut[T](res: Option[T])(exec: T => Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    res.map(exec).getOrElse(Future(Left(ApiError.notFound())))
  def onSuccess[T](res: WriteResult)(default: Either[ApiError, T]): Either[ApiError, T] =
    ApiError.from(res).map { error => Left(error) }.getOrElse(default)
  def onSuccessFut[T](res: WriteResult)(default: Future[Either[ApiError, T]])(implicit ec: ExecutionContext): Future[Either[ApiError, T]] =
    ApiError.from(res).map { error => Future(Left(error)) }.getOrElse(default)

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
