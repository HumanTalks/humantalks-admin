package com.humantalks.venues

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.meetups.MeetupRepository
import com.humantalks.venues.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class VenueCtrl(ctx: Contexts, meetupRepository: MeetupRepository, venueRepository: VenueRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val venueForm = Form(Venue.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    venueRepository.find().map { venueList =>
      Ok(html.list(venueList))
    }
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, venueForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      venueData => venueRepository.create(venueData, User.fake).map {
        case (_, id) => Redirect(routes.VenueCtrl.get(id))
      }
    )
  }

  def get(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueRepository)(id) { venue =>
      for {
        meetupList <- meetupRepository.findForVenue(id)
      } yield Ok(html.detail(venue, meetupList))
    }
  }

  def update(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueRepository)(id) { venue =>
      formView(Ok, venueForm.fill(venue.data), Some(venue))
    }
  }

  def doUpdate(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(venueRepository)(id) { venue => formView(BadRequest, formWithErrors, Some(venue)) },
      venueData => CtrlHelper.withItem(venueRepository)(id) { venue =>
        venueRepository.update(venue, venueData, User.fake).map {
          case _ => Redirect(routes.VenueCtrl.get(id))
        }
      }
    )
  }

  private def formView(status: Status, venueForm: Form[Venue.Data], venueOpt: Option[Venue]): Future[Result] = {
    Future(status(views.html.form(venueForm, venueOpt)))
  }
}
