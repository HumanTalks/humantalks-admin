package com.humantalks.internal.venues

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.events.EventDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class VenueCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    eventDbService: EventDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val venueForm = Form(Venue.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    venueDbService.find().map { venueList =>
      Ok(views.html.list(venueList))
    }
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    formView(Ok, venueForm, None)
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    venueForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      venueData => venueDbService.create(venueData, req.identity.id).map {
        case (_, id) => Redirect(routes.VenueCtrl.get(id))
      }
    )
  }

  def get(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(venueDbService)(id) { venue =>
      for {
        eventList <- eventDbService.findForVenue(id)
        personList <- personDbService.findByIds(venue.data.contacts)
      } yield Ok(views.html.detail(venue, eventList, personList))
    }
  }

  def update(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(venueDbService)(id) { venue =>
      formView(Ok, venueForm.fill(venue.data), Some(venue))
    }
  }

  def doUpdate(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    venueForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(venueDbService)(id) { venue => formView(BadRequest, formWithErrors, Some(venue)) },
      venueData => CtrlHelper.withItem(venueDbService)(id) { venue =>
        venueDbService.update(venue, venueData, req.identity.id).map {
          case _ => Redirect(routes.VenueCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    venueDbService.delete(id).map {
      case Left(events) => Redirect(routes.VenueCtrl.get(id)).flashing("error" -> s"Unable to delete venue, it's still referenced in ${events.length} meetups, delete them first.")
      case Right(res) => Redirect(routes.VenueCtrl.find()).flashing("success" -> "Venue deleted")
    }
  }

  private def formView(status: Status, venueForm: Form[Venue.Data], venueOpt: Option[Venue])(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(venueForm, venueOpt, personForm)))
  }
}
