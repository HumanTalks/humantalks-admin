package com.humantalks.common.services.meetup

import com.humantalks.common.Conf
import com.humantalks.common.services.meetup.models._
import global.Contexts
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class MeetupApi(conf: Conf, ctx: Contexts, ws: WSClient) {
  import Contexts.wsToEC
  import ctx._
  private val baseUrl = "https://api.meetup.com"
  private val authParams = Map("key" -> conf.Meetup.apiKey, "sign" -> "sign")

  def getGroup(groupUrlName: String): Future[Either[List[String], MeetupGroup]] =
    ws.url(s"$baseUrl/$groupUrlName")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[MeetupGroup]))

  def getVenues(groupUrlName: String): Future[Either[List[String], List[MeetupVenue]]] =
    ws.url(s"$baseUrl/$groupUrlName/venues")
      .withQueryString((authParams ++ Map("page" -> "50")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[MeetupVenue]]))

  def createVenue(groupUrlName: String, data: MeetupVenueCreate): Future[Either[List[String], MeetupVenue]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/venues")
        .withQueryString((authParams ++ data.toParams).toList: _*)
        .post("")
        .map(res => parseCreateVenueResponse(res.json))
    } else {
      Future.successful(Left(List(s"createVenue forbidden in '${conf.App.env}'")))
    }

  def getEvents(groupUrlName: String): Future[Either[List[String], List[MeetupEvent]]] =
    ws.url(s"$baseUrl/$groupUrlName/events")
      .withQueryString((authParams ++ Map("photo-host" -> "secure", "status" -> "upcoming,past")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[MeetupEvent]]))

  def getEvent(groupUrlName: String, eventId: String): Future[Either[List[String], MeetupEvent]] =
    ws.url(s"$baseUrl/$groupUrlName/events/$eventId")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[MeetupEvent]))

  def createEvent(groupUrlName: String, data: MeetupEventCreate): Future[Either[List[String], MeetupEvent]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/events")
        .withQueryString((authParams ++ data.toParams).toList: _*)
        .post("")
        .map(res => parseResponse(res.json, _.as[MeetupEvent]))
    } else {
      Future.successful(Left(List(s"createEvent forbidden in '${conf.App.env}'")))
    }

  def updateEvent(groupUrlName: String, eventId: Long, data: MeetupEventCreate): Future[Either[List[String], MeetupEvent]] =
    patchEvent(groupUrlName, eventId, data.toParams)

  def announceEvent(groupUrlName: String, eventId: Long): Future[Either[List[String], MeetupEvent]] =
    patchEvent(groupUrlName, eventId, Map("announce" -> "true", "status" -> "upcoming"))

  private def patchEvent(groupUrlName: String, eventId: Long, data: Map[String, String]): Future[Either[List[String], MeetupEvent]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/events/$eventId")
        .withQueryString((authParams ++ data).toList: _*)
        .patch("")
        .map(res => parseResponse(res.json, _.as[MeetupEvent]))
    } else {
      Future.successful(Left(List(s"updateEvent forbidden in '${conf.App.env}'")))
    }

  def deleteEvent(groupUrlName: String, eventId: Long): Future[Either[List[String], Unit]] =
    if (conf.App.isProd) {
      ws.url(s"$baseUrl/$groupUrlName/events/$eventId")
        .delete()
        .map(res => parseResponse(res.json, _ => ()))
    } else {
      Future.successful(Left(List(s"updateEvent forbidden in '${conf.App.env}'")))
    }

  def getRsvps(groupUrlName: String, eventId: Long): Future[Either[List[String], List[MeetupRsvp]]] =
    ws.url(s"$baseUrl/$groupUrlName/events/$eventId/rsvps")
      .withQueryString((authParams ++ Map("photo-host" -> "secure")).toList: _*)
      .get()
      .map(res => parseResponse(res.json, _.as[List[MeetupRsvp]]))

  private def parseResponse[T](json: JsValue, parse: JsValue => T): Either[List[String], T] =
    (json \ "errors").asOpt[List[JsValue]].map(errs => Left(errs.flatMap(err => (err \ "message").asOpt[String])))
      .orElse { (json \ "error").asOpt[String].map(err => Left(List(err))) }
      .getOrElse { Right(parse(json)) }

  private def parseCreateVenueResponse(json: JsValue): Either[List[String], MeetupVenue] =
    (json \ "errors").asOpt[List[JsValue]].flatMap(_.flatMap(err => (err \ "potential_matches").asOpt[List[MeetupVenue]]).flatten.headOption).map(venue => Right(venue))
      .getOrElse(parseResponse(json, _.as[MeetupVenue]))
}
