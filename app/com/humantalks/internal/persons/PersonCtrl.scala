package com.humantalks.internal.persons

import com.humantalks.auth.entities.User
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.talks.{ TalkDbService, TalkRepository }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class PersonCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    talkDbService: TalkDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction.async { implicit req =>
    personDbService.find().map { personList =>
      Ok(views.html.list(personList))
    }
  }

  def create = silhouette.SecuredAction.async { implicit req =>
    formView(Ok, personForm, None)
  }

  def doCreate() = silhouette.SecuredAction.async { implicit req =>
    personForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      personData => personDbService.create(personData, req.identity.id).map {
        case (_, id) => Redirect(routes.PersonCtrl.get(id))
      }
    )
  }

  def get(id: Person.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(personDbService)(id) { person =>
      for {
        talkList <- talkDbService.findForPerson(id)
      } yield Ok(views.html.detail(person, talkList))
    }
  }

  def update(id: Person.Id) = silhouette.SecuredAction.async { implicit req =>
    CtrlHelper.withItem(personDbService)(id) { person =>
      formView(Ok, personForm.fill(person.data), Some(person))
    }
  }

  def doUpdate(id: Person.Id) = silhouette.SecuredAction.async { implicit req =>
    personForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(personDbService)(id) { person => formView(BadRequest, formWithErrors, Some(person)) },
      personData => CtrlHelper.withItem(personDbService)(id) { person =>
        personDbService.update(person, personData, req.identity.id).map {
          case _ => Redirect(routes.PersonCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Person.Id) = silhouette.SecuredAction.async { implicit req =>
    personDbService.delete(id).map {
      _ match {
        case Left(talks) => Redirect(routes.PersonCtrl.get(id))
        case Right(res) => Redirect(routes.PersonCtrl.find())
      }
    }
  }

  private def formView(status: Status, personForm: Form[Person.Data], personOpt: Option[Person]): Future[Result] = {
    Future(status(views.html.form(personForm, personOpt)))
  }
}
