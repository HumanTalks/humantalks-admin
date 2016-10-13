package com.humantalks.meetups

import com.humantalks.auth.models.User
import com.humantalks.persons.{ Person, PersonRepository }
import com.humantalks.talks.{ Talk, TalkRepository }
import com.humantalks.venues.VenueRepository
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(ctx: Contexts, talkRepository: TalkRepository, personRepository: PersonRepository, venueRepository: VenueRepository, meetupDbService: MeetupDbService)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      meetupList <- meetupDbService.find()
      venueList <- venueRepository.findByIds(meetupList.flatMap(_.data.venue))
    } yield Ok(views.html.list(meetupList, venueList))
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, meetupForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => meetupDbService.create(meetupData, User.fake).map {
        case (_, id) => Redirect(routes.MeetupCtrl.get(id))
      }
    )
  }

  def get(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val allTalksFut = talkRepository.find()
      val allPersonsFut = personRepository.find()
      for {
        venueList <- venueListFut
        allTalks <- allTalksFut
        allPersons <- allPersonsFut
      } yield Ok(views.html.detail(meetup, talkForm, personForm, allTalks, allPersons, venueList))
    }
  }

  def update(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      formView(Ok, meetupForm.fill(meetup.data), Some(meetup))
    }
  }

  def doUpdate(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(meetupDbService)(id) { meetup => formView(BadRequest, formWithErrors, Some(meetup)) },
      meetupData => CtrlHelper.withItem(meetupDbService)(id) { meetup =>
        meetupDbService.update(meetup, meetupData, User.fake).map {
          case _ => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doCreateTalk(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(routes.MeetupCtrl.get(id))), // TODO : add flashing message to show errors
      talkData => talkRepository.create(talkData, User.fake).flatMap {
        case (_, talkId) => meetupDbService.addTalk(id, talkId).map { _ =>
          Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doAddTalkForm(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    req.body.asFormUrlEncoded.get("talkId").headOption.flatMap(p => Talk.Id.from(p).right.toOption).map { talkId =>
      meetupDbService.addTalk(id, talkId).map { _ =>
        Redirect(routes.MeetupCtrl.get(id))
      }
    }.getOrElse {
      Future(Redirect(routes.MeetupCtrl.get(id))) // TODO : add flashing error message
    }
  }

  def doAddTalk(id: Meetup.Id, talkId: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupDbService.addTalk(id, talkId).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doRemoveTalk(id: Meetup.Id, talkId: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupDbService.removeTalk(id, talkId).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def publish(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkRepository.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personRepository.findByIds(talkList.flatMap(_.data.speakers))
      } yield Ok(views.html.publish(meetup, talkList, personList, venueList))
    }
  }

  def doPublish(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupDbService.setPublished(id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  private def formView(status: Status, meetupForm: Form[Meetup.Data], meetupOpt: Option[Meetup]): Future[Result] = {
    val allTalksFut = talkRepository.find()
    val allPersonsFut = personRepository.find()
    val allVenuesFut = venueRepository.find()
    for {
      allTalks <- allTalksFut
      allPersons <- allPersonsFut
      allVenues <- allVenuesFut
    } yield status(views.html.form(meetupForm, talkForm, personForm, meetupOpt, allTalks, allPersons, allVenues))
  }
}
