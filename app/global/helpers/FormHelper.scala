package global.helpers

import play.api.data.Forms._
import play.api.data.format.{ Formats, Formatter }
import play.api.data.{ FormError, Mapping }

object FormHelper {
  /**
   * Helper for formatters binders
   *
   * @param parse Function parsing a String value into a T value, throwing an exception in case of failure
   * @param errArgs Error to set in case of parsing failure
   * @param key Key name of the field to parse
   * @param data Field data
   */
  private def parsing[T](parse: String => T, errMsg: String, errArgs: Seq[Any])(key: String, data: Map[String, String]): Either[Seq[FormError], T] = {
    Formats.stringFormat.bind(key, data).right.flatMap { s =>
      scala.util.control.Exception.allCatch[T]
        .either(parse(s))
        .left.map(e => Seq(FormError(key, errMsg, errArgs)))
    }
  }

  /**
   * Formatter for the `org.joda.time.LocalTime` type.
   *
   * @param pattern a time pattern as specified in `org.joda.time.format.DateTimeFormat`.
   */
  def jodaLocalTimeFormat(pattern: String): Formatter[org.joda.time.LocalTime] = new Formatter[org.joda.time.LocalTime] {
    import org.joda.time.LocalTime

    val formatter = org.joda.time.format.DateTimeFormat.forPattern(pattern)
    def jodaLocalTimeParse(data: String) = LocalTime.parse(data, formatter)

    override val format = Some(("format.time", Seq(pattern)))

    def bind(key: String, data: Map[String, String]) = parsing(jodaLocalTimeParse, "error.time", Nil)(key, data)

    def unbind(key: String, value: LocalTime) = Map(key -> value.toString(pattern))
  }

  /**
   * Default formatter for `org.joda.time.LocalTime` type with pattern `HH:mm`.
   */
  implicit val jodaLocalTimeFormat: Formatter[org.joda.time.LocalTime] = jodaLocalTimeFormat("HH:mm")

  /**
   * Constructs a simple mapping for a date field (mapped as `org.joda.time.LocalTime type`).
   *
   * For example:
   * {{{
   * Form("birthdate" -> jodaLocalTime("HH:mm"))
   * }}}
   *
   * @param pattern the time pattern, as defined in `org.joda.time.format.DateTimeFormat`
   */
  def jodaLocalTime(pattern: String): Mapping[org.joda.time.LocalTime] = of[org.joda.time.LocalTime] as jodaLocalTimeFormat(pattern)
}
