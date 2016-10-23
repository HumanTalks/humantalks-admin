package com.humantalks.exposed

import com.humantalks.exposed.entities.{ PublicVenue, PublicPerson, PublicMeetup, PublicTalk }
import com.humantalks.internal.meetups.{ Meetup, MeetupDbService }
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.{ Talk, TalkDbService }
import com.humantalks.internal.venues.{ Venue, VenueDbService }
import global.Contexts
import global.helpers.ApiHelper
import global.values.ApiError
import org.joda.time.DateTime
import play.api.libs.json.Json
import play.api.mvc.{ Results, Action, Controller }

import scala.concurrent.Future

case class PublicApi(
    ctx: Contexts,
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._
  private def getIncludeList(include: Option[String]): List[String] = include.map(_.split(",").toList.map(_.trim)).getOrElse(List())
  private val includeVenue = "venue"
  private val includeSpeaker = "speaker"
  private val includeTalk = "talk"
  private val includeMeetup = "meetup"

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

  def findMeetups(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        meetups <- meetupDbService.findPublished()
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetups.flatMap(_.data.venue)) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(meetups.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield Right(meetups.map { meetup =>
        PublicMeetup.from(
          meetup,
          if (includeList.contains(includeVenue)) Some(venues) else None,
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
        meetups <- meetupDbService.findPublished()
        talks <- talkDbService.findByIds(meetups.flatMap(_.data.talks))
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetups.flatMap(_.data.venue)) else Future.successful(List())
      } yield Right(talks.map { talk =>
        PublicTalk.from(
          talk,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeVenue)) Some(venues) else None
        )
      })
    })
  }

  def findSpeakers(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        meetups <- meetupDbService.findPublished()
        talks <- talkDbService.findByIds(meetups.flatMap(_.data.talks))
        speakers <- personDbService.findByIds(talks.flatMap(_.data.speakers))
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetups.flatMap(_.data.venue)) else Future.successful(List())
      } yield Right(speakers.map { speaker =>
        PublicPerson.from(
          speaker,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeVenue)) Some(venues) else None
        )
      })
    })
  }

  def findVenues(include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.resultList({
      for {
        venues <- venueDbService.find()
        meetups <- if (includeList.contains(includeMeetup)) meetupDbService.findPublished() else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(meetups.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield Right(venues.map { venue =>
        PublicVenue.from(
          venue,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        )
      })
    })
  }

  def findMeetup(id: Meetup.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        meetupOpt <- meetupDbService.get(id)
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetupOpt.flatMap(_.data.venue).toList) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(meetupOpt.map(_.data.talks).getOrElse(List())) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield meetupOpt.map { meetup =>
        Right(PublicMeetup.from(
          meetup,
          if (includeList.contains(includeVenue)) Some(venues) else None,
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
        meetups <- if (includeList.contains(includeMeetup)) meetupDbService.findForTalk(id) else Future.successful(List())
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetups.flatMap(_.data.venue)) else Future.successful(List())
      } yield talkOpt.map { talk =>
        Right(PublicTalk.from(
          talk,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeVenue)) Some(venues) else None
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
        meetups <- if (includeList.contains(includeMeetup)) meetupDbService.findForTalks(talks.map(_.id)) else Future.successful(List())
        venues <- if (includeList.contains(includeVenue)) venueDbService.findByIds(meetups.flatMap(_.data.venue)) else Future.successful(List())
      } yield speakerOpt.map { speaker =>
        Right(PublicPerson.from(
          speaker,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeVenue)) Some(venues) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }

  def findVenue(id: Venue.Id, include: Option[String]) = Action.async { implicit req =>
    val includeList = getIncludeList(include)
    ApiHelper.result({
      for {
        venueOpt <- venueDbService.get(id)
        meetups <- if (includeList.contains(includeMeetup)) meetupDbService.findForVenue(id) else Future.successful(List())
        talks <- if (includeList.contains(includeTalk)) talkDbService.findByIds(meetups.flatMap(_.data.talks)) else Future.successful(List())
        speakers <- if (includeList.contains(includeSpeaker)) personDbService.findByIds(talks.flatMap(_.data.speakers)) else Future.successful(List())
      } yield venueOpt.map { venue =>
        Right(PublicVenue.from(
          venue,
          if (includeList.contains(includeMeetup)) Some(meetups) else None,
          if (includeList.contains(includeTalk)) Some(talks) else None,
          if (includeList.contains(includeSpeaker)) Some(speakers) else None
        ))
      }.getOrElse {
        Left(ApiError.notFound())
      }
    })
  }
}
