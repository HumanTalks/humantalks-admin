package com.humantalks.internal.meetups

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ TalkDbService, Talk }
import com.humantalks.internal.venues.VenueDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction.async { implicit req =>
    for {
      meetupList <- meetupDbService.find()
      venueList <- venueDbService.findByIds(meetupList.flatMap(_.data.venue))
    } yield Ok(views.html.list(meetupList, venueList))
  }

  def create = silhouette.SecuredAction.async { implicit req =>
    formView(Ok, meetupForm, None)
  }

  def doCreate() = silhouette.SecuredAction.async { implicit req =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => meetupDbService.create(meetupData, req.identity.id).map {
        case (_, id) => Redirect(routes.MeetupCtrl.get(id))
      }
    )
  }

  def get(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueDbService.findByIds(meetup.data.venue.toSeq)
      val allTalksFut = talkDbService.find()
      val allPersonsFut = personDbService.find()
      for {
        venueList <- venueListFut
        allTalks <- allTalksFut
        allPersons <- allPersonsFut
      } yield Ok(views.html.detail(meetup, talkForm, personForm, allTalks, allPersons, venueList))
    }
  }

  def update(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      formView(Ok, meetupForm.fill(meetup.data), Some(meetup))
    }
  }

  def doUpdate(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(meetupDbService)(id) { meetup => formView(BadRequest, formWithErrors, Some(meetup)) },
      meetupData => CtrlHelper.withItem(meetupDbService)(id) { meetup =>
        meetupDbService.update(meetup, meetupData, req.identity.id).map {
          case _ => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doCreateTalk(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => Future(Redirect(routes.MeetupCtrl.get(id))), // TODO : add flashing message to show errors
      talkData => talkDbService.create(talkData, req.identity.id).flatMap {
        case (_, talkId) => meetupDbService.addTalk(id, talkId).map { _ =>
          Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doAddTalkForm(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    req.body.asFormUrlEncoded.get("talkId").headOption.flatMap(p => Talk.Id.from(p).right.toOption).map { talkId =>
      meetupDbService.addTalk(id, talkId).map { _ =>
        Redirect(routes.MeetupCtrl.get(id))
      }
    }.getOrElse {
      Future(Redirect(routes.MeetupCtrl.get(id))) // TODO : add flashing error message
    }
  }

  def doAddTalk(id: Meetup.Id, talkId: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    meetupDbService.addTalk(id, talkId).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doRemoveTalk(id: Meetup.Id, talkId: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    meetupDbService.removeTalk(id, talkId).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def publish(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueDbService.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkDbService.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
      } yield Ok(views.html.publish(meetup, talkList, personList, venueList))
    }
  }

  def doPublish(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    meetupDbService.setPublished(id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doDelete(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req =>
    meetupDbService.delete(id).map {
      _ match {
        case Left(nothing) => Redirect(routes.MeetupCtrl.get(id))
        case Right(res) => Redirect(routes.MeetupCtrl.find())
      }
    }
  }

  private def formView(status: Status, meetupForm: Form[Meetup.Data], meetupOpt: Option[Meetup]): Future[Result] = {
    val allTalksFut = talkDbService.find()
    val allPersonsFut = personDbService.find()
    val allVenuesFut = venueDbService.find()
    for {
      allTalks <- allTalksFut
      allPersons <- allPersonsFut
      allVenues <- allVenuesFut
    } yield status(views.html.form(meetupForm, talkForm, personForm, meetupOpt, allTalks, allPersons, allVenues))
  }
}
