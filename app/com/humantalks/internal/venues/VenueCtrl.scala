package com.humantalks.internal.venues

import com.humantalks.auth.silhouette.User
import com.humantalks.internal.meetups.MeetupDbService
import com.humantalks.internal.persons.PersonDbService
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class VenueCtrl(ctx: Contexts, venueDbService: VenueDbService, personDbService: PersonDbService, meetupDbService: MeetupDbService)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val venueForm = Form(Venue.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    venueDbService.find().map { venueList =>
      Ok(views.html.list(venueList))
    }
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, venueForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      venueData => venueDbService.create(venueData, User.fake).map {
        case (_, id) => Redirect(routes.VenueCtrl.get(id))
      }
    )
  }

  def get(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueDbService)(id) { venue =>
      for {
        meetupList <- meetupDbService.findForVenue(id)
      } yield Ok(views.html.detail(venue, meetupList))
    }
  }

  def update(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueDbService)(id) { venue =>
      formView(Ok, venueForm.fill(venue.data), Some(venue))
    }
  }

  def doUpdate(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(venueDbService)(id) { venue => formView(BadRequest, formWithErrors, Some(venue)) },
      venueData => CtrlHelper.withItem(venueDbService)(id) { venue =>
        venueDbService.update(venue, venueData, User.fake).map {
          case _ => Redirect(routes.VenueCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    venueDbService.delete(id).map {
      _ match {
        case Left(meetups) => Redirect(routes.VenueCtrl.get(id))
        case Right(res) => Redirect(routes.VenueCtrl.find())
      }
    }
  }

  private def formView(status: Status, venueForm: Form[Venue.Data], venueOpt: Option[Venue]): Future[Result] = {
    personDbService.find().map { personList =>
      status(views.html.form(venueForm, venueOpt, personList))
    }
  }
}
