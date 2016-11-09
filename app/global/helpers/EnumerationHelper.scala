package global.helpers

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.libs.json._

import scala.language.implicitConversions
import scala.util.{ Failure, Success, Try }

object EnumerationHelper {
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

  // read value from Play Form
  implicit def formMapping[E <: Enumeration](enum: E): Formatter[E#Value] = new Formatter[E#Value] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], E#Value] =
      data.get(key).map { str =>
        Try(enum.withName(str)) match {
          case Success(value) => Right(value)
          case Failure(err) => Left(Seq(FormError(key, s"Incorrect Enum '$str' for ${enum.getClass}", Nil)))
        }
      }.getOrElse(Left(Seq(FormError(key, "error.wrongFormat", Nil))))
    override def unbind(key: String, value: E#Value): Map[String, String] = Map(key -> value.toString)
  }
}
