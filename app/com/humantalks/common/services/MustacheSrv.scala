package com.humantalks.common.services

import java.io.{ StringReader, StringWriter }
import play.api.libs.json._

import scala.collection.JavaConverters._

import com.github.mustachejava.DefaultMustacheFactory

import scala.util.Try

object MustacheSrv {
  def build(template: String, scopes: Map[String, JsValue]): Try[String] = Try {
    val writer = new StringWriter()
    val mustacheFactory = new DefaultMustacheFactory()
    val mustache = mustacheFactory.compile(new StringReader(template), "mustache-template")
    mustache.execute(writer, scopes.filter(_._2 != JsNull).map { case (key, value) => (key, format(value)) }.asJava)
    writer.toString.trim
  }

  private def format(json: JsValue): Object = {
    (json match {
      case JsObject(obj) => obj.map { case (key, value) => (key, format(value)) }.asJava
      case JsArray(arr) => arr.map(format).asJava
      case JsString(str) => str
      case JsNumber(n) => n
      case JsBoolean(b) => b
      case JsNull => null
    }).asInstanceOf[Object]
  }
}
