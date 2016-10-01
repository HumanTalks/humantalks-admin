package com.humantalks.common.helpers

import play.api.libs.json.{ Json, Writes }
import play.api.libs.ws.{ WSClient, WSResponse }
import play.api.mvc.Result
import play.api.mvc.Results._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

object ScraperHelper {
  def toInt(str: String): Option[Int] = Try(str.replaceAll("[^\\d.]", "").toInt).toOption

  def fetchHtml(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[String] =
    fetch(ws)(_.body)(url)

  def scrapeHtml[T](ws: WSClient)(url: String)(parser: (String, String) => T)(implicit w: Writes[T], ec: ExecutionContext): Future[Result] =
    scrape(fetchHtml(ws))(url)(parser)

  def format[T](result: T)(implicit w: Writes[T]): Result =
    Ok(Json.obj(
      "data" -> result
    )).withHeaders("Content-Type" -> "application/json; charset=utf-8")

  /*
    Private methods
   */

  private def fetch[U](ws: WSClient)(extract: WSResponse => U)(url: String)(implicit ec: ExecutionContext): Future[U] =
    ws.url(url).get().map { response => extract(response) }

  private def scrape[T, U](fetch: String => Future[U])(url: String)(parser: (U, String) => T)(implicit w: Writes[T], ec: ExecutionContext): Future[Result] =
    fetch(url)
      .map { value => format(parser(value, url)) }
      .recover {
        case e: Exception =>
          NotFound(Json.obj(
            "message" -> s"Unable to connect to $url",
            "error" -> e.getMessage
          )).withHeaders("Content-Type" -> "application/json; charset=utf-8")
      }
}
