package global.helpers

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.libs.json._
import play.api.mvc.{ QueryStringBindable, PathBindable }

import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

object EnumerationHelper {
  private def from[E <: Enumeration](enum: E)(key: String, str: String): Either[String, E#Value] = Try(enum.withName(str)) match {
    case Success(value) => Right(value)
    case Failure(err) => Left(s"Incorrect Enum '$str' for ${enum.getClass}")
  }
  private def to[E <: Enumeration](enum: E)(value: E#Value): String = value.toString
  private val buildErrKey = "error.wrongFormat"
  private def buildErrMsg[E <: Enumeration](enum: E)(value: String) = s"Wrong value '$value' for Enum ${enum.getClass}"

  // read/write value to JSON
  def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
    def reads(json: JsValue): JsResult[E#Value] = json match {
      case JsString(str) => Try(enum.withName(str)) match {
        case Success(value) => JsSuccess(value)
        case Failure(err) => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$str'")
      }
      case _ => JsError(s"String value expected for Enum '${enum.getClass}'")
    }
  }
  def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
    def writes(value: E#Value): JsValue = JsString(value.toString)
  }
  implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = Format(enumReads(enum), enumWrites)

  // read/write value from URL path
  implicit def pathBinder[E <: Enumeration](enum: E): PathBindable[E#Value] = new PathBindable[E#Value] {
    override def bind(key: String, value: String): Either[String, E#Value] = from(enum)(key, value)
    override def unbind(key: String, value: E#Value): String = to(enum)(value)
  }
  implicit def pathBinderOpt[E <: Enumeration](enum: E): PathBindable[Option[E#Value]] = new PathBindable[Option[E#Value]] {
    override def bind(key: String, value: String): Either[String, Option[E#Value]] = from(enum)(key, value).right.map(Some(_))
    override def unbind(key: String, value: Option[E#Value]): String = value.map(to(enum)).getOrElse("")
  }

  // read/write value from URL query string
  implicit def queryBinder[E <: Enumeration](enum: E): QueryStringBindable[E#Value] = new QueryStringBindable[E#Value] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, E#Value]] =
      params.get(key).map { values => values.headOption.map(v => from(enum)(key, v)).getOrElse(Left(buildErrMsg(enum)(values.toString))) }
    override def unbind(key: String, value: E#Value): String = to(enum)(value)
  }
  implicit def queryBinderOpt[E <: Enumeration](enum: E): QueryStringBindable[Option[E#Value]] = new QueryStringBindable[Option[E#Value]] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Option[E#Value]]] =
      params.get(key).map { values => values.headOption.map(v => from(enum)(key, v).right.map(Some(_))).getOrElse(Left(buildErrMsg(enum)(values.toString))) }
    override def unbind(key: String, value: Option[E#Value]): String = value.map(to(enum)).getOrElse("")
  }

  // read value from Play Form
  implicit def formMapping[E <: Enumeration](enum: E): Formatter[E#Value] = new Formatter[E#Value] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], E#Value] =
      data.get(key).map { value => from(enum)(key, value).left.map(msg => Seq(FormError(key, msg, Nil))) }.getOrElse(Left(Seq(FormError(key, buildErrKey, Nil))))
    override def unbind(key: String, value: E#Value): Map[String, String] = Map(key -> to(enum)(value))
  }
}
