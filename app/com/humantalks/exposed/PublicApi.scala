package com.humantalks.exposed

import com.humantalks.exposed.entities.{ PublicPartner, PublicPerson, PublicEvent, PublicTalk }
import com.humantalks.internal.events.{ Event, EventDbService }
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.{ Talk, TalkDbService }
import com.humantalks.internal.partners.{ Partner, PartnerDbService }
import global.Contexts
import global.helpers.ApiHelper
import global.values.ApiError
import play.api.libs.json.Json
import play.api.mvc.{ Results, Action, Controller }

import scala.concurrent.Future

case class PublicApi(
    ctx: Contexts,
    partnerDbService: PartnerDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    eventDbService: EventDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._
  private def getIncludeList(include: Option[String]): List[String] = include.map(_.split(",").toList.map(_.trim)).getOrElse(List())
  private val includePartner = "venue"
  private val includeSpeaker = "speaker"
  private val includeTalk = "talk"
  private val includeEvent = "meetup"

  def apiRoot = Action.async { implicit req =>
    ApiHelper.resultJson(Future.successful(Right(Json.obj(
      "api" -> "exposedApi",
      "endpoints" -> Json.obj(
        "GET /meetups" -> "find all meetups",
        "GET /meetups/:id" -> "get meetup by id",
        "GET /talks" -> "find all talks",
        "GET /talks/:id" -> "get talk by id",
        "GET /speakers" -> "find all speakers",
        "GET /speakers/:id" -> "get speaker by id",
        "GET /venues" -> "find all venues",
        "GET /venues/:id" -> "get venue by id"
      ),
      "comment" -> "All endpoints take an 'include' query string which embeds related values inside the response. Ex: /meetups?include=talk,speaker"
    ))), Results.Ok, Results.InternalServerError)
  }

  def createPerson = Action.async(parse.json) { implicit req => ApiHelper.create(personDbService, Person.anonymous, req.body) }

  def findEvents(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        events <- eventDbService.findPast()
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(events.flatMap(_.data.venue)) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(events.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield Right(events.map { event =>
        PublicEvent.from(
          event,
          if (includeList.contains(includePartner)) Some(partners) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        )
      })
    })
  }

  def findTalks(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        events <- eventDbService.findPast()
        talks <- talkDbService.findByIds(events.flatMap(_.data.talks))
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(events.flatMap(_.data.venue)) else Future.successful(List())
      } yield Right(talks.map { talk =>
        PublicTalk.from(
          talk,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includePartner)) Some(partners) else None
        )
      })
    })
  }

  def findSpeakers(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        events <- eventDbService.findPast()
        talks <- talkDbService.findByIds(events.flatMap(_.data.talks))
        speakers <- personDbService.findByIds(talks.flatMap(_.data.speakers))
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(events.flatMap(_.data.venue)) else Future.successful(List())
      } yield Right(speakers.map { speaker =>
        PublicPerson.from(
          speaker,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includePartner)) Some(partners) else None
        )
      })
    })
  }

  def findPartners(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        partners <- partnerDbService.find()
        events <- if (includeList.contains(includeEvent)) eventDbService.findPast() else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(events.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield Right(partners.map { partner =>
        PublicPartner.from(
          partner,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        )
      })
    })
  }

  def findEvent(id: Event.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        eventOpt <- eventDbService.get(id)
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(eventOpt.flatMap(_.data.venue).toList) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(eventOpt.map(_.data.talks).getOrElse(List())) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield eventOpt.map { event =>
        Right(PublicEvent.from(
          event,
          if (includeList.contains(includePartner)) Some(partners) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }

  def findTalk(id: Talk.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        talkOpt <- talkDbService.get(id)
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talkOpt.map(_.data.speakers).getOrElse(List())) else Future.successful(List())
        events <- if (includeList.contains(includeEvent)) eventDbService.findForTalk(id) else Future.successful(List())
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(events.flatMap(_.data.venue)) else Future.successful(List())
      } yield talkOpt.map { talk =>
        Right(PublicTalk.from(
          talk,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includePartner)) Some(partners) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }

  def findSpeaker(id: Person.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        speakerOpt <- personDbService.get(id)
        talks <- if (includeList.contains(includeTalk)) talkDbService.findForPerson(id) else Future.successful(List())
        events <- if (includeList.contains(includeEvent)) eventDbService.findForTalks(talks.map(_.id)) else Future.successful(List())
        partners <- if (includeList.contains(includePartner)) partnerDbService.findByIds(events.flatMap(_.data.venue)) else Future.successful(List())
      } yield speakerOpt.map { speaker =>
        Right(PublicPerson.from(
          speaker,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includePartner)) Some(partners) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }

  def findPartner(id: Partner.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        partnerOpt <- partnerDbService.get(id)
        events <- if (includeList.contains(includeEvent)) eventDbService.findForPartner(id) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(events.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield partnerOpt.map { partner =>
        Right(PublicPartner.from(
          partner,
          if (includeList.contains(includeEvent)) Some(events) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }
}
