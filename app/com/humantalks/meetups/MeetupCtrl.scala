package com.humantalks.meetups

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.persons.PersonRepository
import com.humantalks.talks.{ Talk, TalkRepository }
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
  val talkForm = Form(Talk.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      meetupList <- meetupRepository.find()
      venueList <- venueRepository.findByIds(meetupList.flatMap(_.data.venue))
    } yield Ok(html.list(meetupList, venueList))
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, meetupForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => meetupRepository.create(meetupData, User.fake).map {
        case (_, id) => Redirect(routes.MeetupCtrl.get(id))
      }
    )
  }

  def get(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupRepository)(id) { meetup =>
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkRepository.findByIds(meetup.data.talks)
      val personListFut = personRepository.find()
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personListFut
      } yield Ok(html.detail(meetup, talkList, personList, venueList, talkForm))
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
          case _ => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doCreateTalk(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(routes.MeetupCtrl.get(id))), // TODO : add flashing message to show errors
      talkData => talkRepository.create(talkData, User.fake).flatMap {
        case (_, talkId) => meetupRepository.addTalk(id, talkId).map { _ =>
          Redirect(routes.MeetupCtrl.get(id))
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
      } yield Ok(html.publish(meetup, talkList, personList, venueList))
    }
  }

  def doPublish(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupRepository.setPublished(id).map { _ =>
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
    } yield status(html.form(meetupForm, meetupOpt, talkList, personList, venueList))
  }
}
