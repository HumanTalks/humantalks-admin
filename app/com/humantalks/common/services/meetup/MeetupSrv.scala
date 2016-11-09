package com.humantalks.common.services.meetup

import com.humantalks.common.Conf
import global.Contexts
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class MeetupSrv(conf: Conf, ctx: Contexts, ws: WSClient) {
  import Contexts.wsToEC
  import ctx._
  private val baseUrl = "https://api.meetup.com"
  private val authParams = Map("key" -> conf.Meetup.apiKey, "sign" -> "sign")

  def getGroup(groupUrlName: String): Future[Either[List[String], Group]] =
    ws.url(s"$baseUrl/$groupUrlName")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[Group]))

  def getVenues(groupUrlName: String): Future[Either[List[String], List[Venue]]] =
    ws.url(s"$baseUrl/$groupUrlName/venues")
      .withQueryString((authParams ++ Map("page" -> "50")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[Venue]]))

  def getEvents(groupUrlName: String): Future[Either[List[String], List[Event]]] =
    ws.url(s"$baseUrl/$groupUrlName/events")
      .withQueryString((authParams ++ Map("photo-host" -> "secure", "status" -> "upcoming,past")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[Event]]))

  def getEvent(groupUrlName: String, eventId: String): Future[Either[List[String], Event]] =
    ws.url(s"$baseUrl/$groupUrlName/events/$eventId")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[Event]))

  def createEvent(groupUrlName: String, data: EventCreate): Future[Either[List[String], Event]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/events")
        .withQueryString((authParams ++ data.toParams).toList: _*)
        .post("")
        .map(res => parseResponse(res.json, _.as[Event]))
    } else {
      Future.successful(Left(List(s"createEvent forbidden in '${conf.App.env}'")))
    }

  def updateEvent(groupUrlName: String, eventId: String, data: Map[String, String]): Future[Either[List[String], Event]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/events/$eventId")
        .withQueryString((authParams ++ data).toList: _*)
        .patch("")
        .map(res => parseResponse(res.json, _.as[Event]))
    } else {
      Future.successful(Left(List(s"updateEvent forbidden in '${conf.App.env}'")))
    }

  def getRsvps(groupUrlName: String, eventId: String): Future[Either[List[String], List[Rsvp]]] =
    ws.url(s"$baseUrl/$groupUrlName/events/$eventId/rsvps")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[Rsvp]]))

  private def parseResponse[T](json: JsValue, parse: JsValue => T): Either[List[String], T] =
    (json \ "errors").asOpt[List[JsValue]].map(errs => Left(errs.flatMap(err => (err \ "message").asOpt[String])))
      .orElse { (json \ "error").asOpt[String].map(err => Left(List(err))) }
      .getOrElse { Right(parse(json)) }
}
