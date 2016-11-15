package com.humantalks.internal.talks

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.exposed.proposals.ProposalDbService
import com.humantalks.internal.meetups.MeetupDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
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
    meetupDbService: MeetupDbService,
    proposalDbService: ProposalDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      talkList <- talkDbService.find()
      personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
    } yield Ok(views.html.list(talkList, personList))
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    formView(Ok, talkForm, None)
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      talkData => talkDbService.create(talkData, req.identity.id).map {
        case (_, id) => Redirect(routes.TalkCtrl.get(id))
      }
    )
  }

  def get(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      for {
        proposalOpt <- proposalDbService.getForTalk(id)
        personList <- personDbService.findByIds(talk.data.speakers)
        meetupList <- meetupDbService.findForTalk(id)
      } yield Ok(views.html.detail(talk, proposalOpt, personList, meetupList))
    }
  }

  def update(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      formView(Ok, talkForm.fill(talk.data), Some(talk))
    }
  }

  def doUpdate(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    talkForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(talkDbService)(id) { talk => formView(BadRequest, formWithErrors, Some(talk)) },
      talkData => CtrlHelper.withItem(talkDbService)(id) { talk =>
        talkDbService.update(talk, talkData, req.identity.id).map {
          case _ => Redirect(routes.TalkCtrl.get(id))
        }
      }
    )
  }

  def setStatus(id: Talk.Id, status: Talk.Status.Value) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    talkDbService.setStatus(id, status, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.TalkCtrl.get(id)))
    }
  }

  def doAccept(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    talkDbService.accept(id, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.TalkCtrl.get(id)))
    }
  }

  def doReject(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    talkDbService.reject(id, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.TalkCtrl.get(id)))
    }
  }

  def setAttribute(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.TalkCtrl.get(id))
    (for {
      attribute <- CtrlHelper.getFieldValue(req.body, "attribute")
      value <- CtrlHelper.getFieldValue(req.body, "value")
    } yield {
      talkDbService.updateAttribute(id, attribute, value, req.identity.id)
        .map { _ => Redirect(redirectUrl) }
        .recover { case e: Throwable => Redirect(redirectUrl).flashing("error" -> s"Bad request: ${e.getMessage}") }
    }).getOrElse {
      Future.successful(Redirect(redirectUrl).flashing("error" -> "Bad request: you should set 'attribute' and 'value' in form parameters !"))
    }
  }

  def doDelete(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    talkDbService.delete(id).map {
      case Left(meetups) => Redirect(routes.TalkCtrl.get(id)).flashing("error" -> s"Unable to delete talk, it's still referenced in ${meetups.length} meetups, delete them first.")
      case Right(res) => Redirect(routes.TalkCtrl.find()).flashing("success" -> "Talk deleted")
    }
  }

  private def formView(status: Status, talkForm: Form[Talk.Data], talkOpt: Option[Talk])(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(talkForm, talkOpt, personForm)))
  }
}
