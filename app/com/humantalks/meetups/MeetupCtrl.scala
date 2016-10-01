package com.humantalks.meetups

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.persons.PersonRepository
import com.humantalks.talks.TalkRepository
import com.humantalks.venues.VenueRepository
import com.humantalks.meetups.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(ctx: Contexts, meetupRepository: MeetupRepository, talkRepository: TalkRepository, personRepository: PersonRepository, venueRepository: VenueRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      meetupList <- meetupRepository.find()
      venueMap <- venueRepository.findByIds(meetupList.flatMap(_.data.venue)).map(_.map(v => (v.id, v)).toMap)
    } yield Ok(html.list(meetupList, venueMap))
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, meetupForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => meetupRepository.create(meetupData, User.fake).map {
        case (success, id) => Redirect(routes.MeetupCtrl.get(id))
      }
    )
  }

  def get(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupRepository)(id) { meetup =>
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkRepository.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personRepository.findByIds(talkList.flatMap(_.data.speakers))
      } yield Ok(html.detail(meetup, talkList.map(e => (e.id, e)).toMap, personList.map(e => (e.id, e)).toMap, venueList.map(e => (e.id, e)).toMap))
    }
  }

  def update(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupRepository)(id) { meetup =>
      formView(Ok, meetupForm.fill(meetup.data), Some(meetup))
    }
  }

  def doUpdate(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(meetupRepository)(id) { meetup => formView(BadRequest, formWithErrors, Some(meetup)) },
      meetupData => CtrlHelper.withItem(meetupRepository)(id) { meetup =>
        meetupRepository.update(meetup, meetupData, User.fake).map {
          case success => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def publish(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupRepository)(id) { meetup =>
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkRepository.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personRepository.findByIds(talkList.flatMap(_.data.speakers))
      } yield Ok(html.publish(meetup, talkList.map(e => (e.id, e)).toMap, personList.map(e => (e.id, e)).toMap, venueList.map(e => (e.id, e)).toMap))
    }
  }

  def doPublish(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupRepository.partialUpdate(id, Json.obj("published" -> true)).map { res =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  private def formView(status: Status, meetupForm: Form[Meetup.Data], meetupOpt: Option[Meetup]): Future[Result] = {
    val talkListFut = talkRepository.find()
    val venueListFut = venueRepository.find()
    for {
      talkList <- talkListFut
      personList <- personRepository.findByIds(talkList.flatMap(_.data.speakers))
      venueList <- venueListFut
    } yield status(html.form(meetupForm, meetupOpt, talkList, personList.map(e => (e.id, e)).toMap, venueList))
  }
}
