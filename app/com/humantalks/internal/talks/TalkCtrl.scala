package com.humantalks.internal.talks

import com.humantalks.auth.entities.User
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.meetups.{ MeetupDbService, MeetupRepository }
import com.humantalks.internal.persons.{ PersonDbService, Person, PersonRepository }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class TalkCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction.async { implicit req =>
    for {
      talkList <- talkDbService.find()
      personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
    } yield Ok(views.html.list(talkList, personList))
  }

  def create = silhouette.SecuredAction.async { implicit req =>
    formView(Ok, talkForm, None)
  }

  def doCreate() = silhouette.SecuredAction.async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      talkData => talkDbService.create(talkData, req.identity.id).map {
        case (_, id) => Redirect(routes.TalkCtrl.get(id))
      }
    )
  }

  def get(id: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      for {
        personList <- personDbService.findByIds(talk.data.speakers)
        meetupList <- meetupDbService.findForTalk(id)
      } yield Ok(views.html.detail(talk, personList, meetupList))
    }
  }

  def update(id: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      formView(Ok, talkForm.fill(talk.data), Some(talk))
    }
  }

  def doUpdate(id: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(talkDbService)(id) { talk => formView(BadRequest, formWithErrors, Some(talk)) },
      talkData => CtrlHelper.withItem(talkDbService)(id) { talk =>
        talkDbService.update(talk, talkData, req.identity.id).map {
          case _ => Redirect(routes.TalkCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Talk.Id) = silhouette.SecuredAction.async { implicit req =>
    talkDbService.delete(id).map {
      _ match {
        case Left(meetups) => Redirect(routes.TalkCtrl.get(id))
        case Right(res) => Redirect(routes.TalkCtrl.find())
      }
    }
  }

  private def formView(status: Status, talkForm: Form[Talk.Data], talkOpt: Option[Talk]): Future[Result] = {
    personDbService.find().map { personList =>
      status(views.html.form(talkForm, talkOpt, personList, personForm))
    }
  }
}
