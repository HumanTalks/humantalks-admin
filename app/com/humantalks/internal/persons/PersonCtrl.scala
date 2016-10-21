package com.humantalks.internal.persons

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.exposed.proposals.ProposalDbService
import com.humantalks.internal.talks.TalkDbService
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
    talkDbService: TalkDbService,
    proposalDbService: ProposalDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    personDbService.find().map { personList =>
      Ok(views.html.list(personList))
    }
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    formView(Ok, personForm, None)
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    personForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      personData => personDbService.create(personData, req.identity.id).map {
        case (_, id) => Redirect(routes.PersonCtrl.get(id))
      }
    )
  }

  def get(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(personDbService)(id) { person =>
      for {
        talkList <- talkDbService.findForPerson(id)
        proposalList <- proposalDbService.findForPerson(id)
      } yield Ok(views.html.detail(person, talkList, proposalList))
    }
  }

  def update(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(personDbService)(id) { person =>
      formView(Ok, personForm.fill(person.data), Some(person))
    }
  }

  def doUpdate(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    personForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(personDbService)(id) { person => formView(BadRequest, formWithErrors, Some(person)) },
      personData => CtrlHelper.withItem(personDbService)(id) { person =>
        personDbService.update(person, personData, req.identity.id).map {
          case _ => Redirect(routes.PersonCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    personDbService.delete(id).map {
      _ match {
        case Left((talks, proposals)) => {
          val references = List(
            if (talks.nonEmpty) Some(talks.length + " talks") else None,
            if (proposals.nonEmpty) Some(proposals.length + " proposals") else None
          ).flatten.mkString(",")
          Redirect(routes.PersonCtrl.get(id)).flashing("error" -> s"Unable to delete person, it's still referenced in $references, delete them first.")
        }
        case Right(res) => Redirect(routes.PersonCtrl.find()).flashing("success" -> "Person deleted")
      }
    }
  }

  def profil = silhouette.SecuredAction.async { implicit req =>
    implicit val user = Some(req.identity)
    Future.successful(Ok(views.html.profil(req.identity)))
  }

  private def formView(status: Status, personForm: Form[Person.Data], personOpt: Option[Person])(implicit request: RequestHeader, user: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(personForm, personOpt)))
  }
}
