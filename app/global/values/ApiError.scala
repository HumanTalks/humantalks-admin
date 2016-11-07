package global.values

import java.net.ConnectException

import global.helpers.EnumerationHelper
import play.api.data.validation.ValidationError
import play.api.libs.json.{ JsPath, Json }
import reactivemongo.api.commands.WriteResult
import reactivemongo.core.actors.Exceptions.NodeSetNotReachable

case class ApiValidationError(
  path: String,
  errors: Seq[String]
)

object ApiValidationError {
  implicit val format = Json.format[ApiValidationError]
  def from(err: (JsPath, Seq[ValidationError])): ApiValidationError = ApiValidationError(
    err._1.toJsonString.replace("obj.", ""),
    err._2.map(_.message.replace("error.", ""))
  )
}

case class ApiError(
  code: ApiError.Code.Code,
  message: String,
  description: Option[String] = None,
  errors: Option[Seq[ApiValidationError]] = None
)

object ApiError {
  object Code extends Enumeration {
    type Code = Value
    val NotFound, Validation, NetworkError, MongoError, MongoWriteError, Unknown = Value
  }

  val emtpy = ApiError(Code.Unknown, "empty")
  def notFound(): ApiError = ApiError(Code.NotFound, "Unable to find requested element")
  def notFound(message: String, description: Option[String] = None): ApiError = ApiError(Code.NotFound, message, description)
  def from(e: Throwable): ApiError = e match {
    case e: ConnectException => ApiError(Code.NetworkError, "Can't access network", Some(e.getMessage))
    case e: NodeSetNotReachable => ApiError(Code.MongoError, "Can't access database", Some(e.getMessage.replace("MongoError['", "").replace("']", "")))
    case _ => ApiError(Code.Unknown, "Unexpected exception: " + e.getClass.getName, Some(e.getMessage))
  }
  def from(res: WriteResult): Option[ApiError] =
    Some(res).filter(!_.ok).map { r => ApiError(Code.MongoWriteError, r.message, r.errmsg) }
  def from(validation: Seq[(JsPath, Seq[ValidationError])]): ApiError = ApiError(
    Code.Validation,
    "Wrong input data",
    None,
    Some(validation.map(ApiValidationError.from))
  )

  implicit val formatService = EnumerationHelper.enumFormat(Code)
  implicit val format = Json.format[ApiError]
}