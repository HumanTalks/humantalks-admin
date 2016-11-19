package global.views

import play.api.data.Field
import play.twirl.api.Html

object Helpers {
  def fieldId(field: Field, args: Seq[(Symbol, String)]): String =
    getArg(args, "id", field.id)

  def fieldName(field: Field, args: Seq[(Symbol, String)]): String =
    getArg(args, "name", field.name)

  def fieldValue(field: Field, args: Seq[(Symbol, String)]): String =
    getArg(args, "value", field.value.getOrElse(""))

  def isRequired(field: Field, args: Seq[(Symbol, String)]): Boolean =
    field.constraints.exists(_._1 == "constraint.required") && getArg(args, "required", "") != "false"

  def isChecked(field: Field, args: Seq[(Symbol, String)]): Boolean =
    field.value.map(_ == "true").getOrElse(hasArg(args, "value", "true"))

  def getArg(args: Seq[(Symbol, String)], arg: String, default: String = ""): String =
    args.find(_._1.name == arg).map(_._2).getOrElse(default)

  def hasArg(args: Seq[(Symbol, String)], arg: String, expectedValue: String = ""): Boolean =
    args.find(_._1.name == arg).exists(_._2 == expectedValue || expectedValue.length == 0)

  def toHtml(args: Seq[(Symbol, String)], exclude: Seq[String] = Seq()): Html = {
    val toExclude = List("id", "name", "value", "class", "label", "required", "checked", "errors") ++ exclude
    Html(args
      .filterNot(e => toExclude.contains(e._1.name))
      .map { case (symbol, value) => symbol.name + "=\"" + value + "\"" }
      .mkString(" "))
  }

  def addArg(args: Seq[(Symbol, String)], key: Symbol, value: String): Seq[(Symbol, String)] =
    args.filter(_._1 != key) :+ args.find(_._1 == key).map(a => (a._1, a._2 + " " + value)).getOrElse(key -> value)
}
