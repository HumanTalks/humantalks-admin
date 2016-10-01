package com.humantalks.venues

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.venues.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class VenueCtrl(ctx: Contexts, venueRepository: VenueRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val venueForm = Form(Venue.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    venueRepository.find().map { venueList =>
      Ok(html.list(venueList))
    }
  }

  def create = Action { implicit req: Request[AnyContent] =>
    Ok(html.form(venueForm, None))
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(html.form(formWithErrors, None))),
      venueData => venueRepository.create(venueData, User.fake).map {
        case (success, id) => Redirect(routes.VenueCtrl.get(id))
      }
    )
  }

  def get(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueRepository)(id) { venue =>
      Future(Ok(html.detail(venue)))
    }
  }

  def update(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(venueRepository)(id) { venue =>
      Future(Ok(html.form(venueForm.fill(venue.data), Some(venue))))
    }
  }

  def doUpdate(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(venueRepository)(id) { venue => Future(BadRequest(html.form(formWithErrors, Some(venue)))) },
      venueData => CtrlHelper.withItem(venueRepository)(id) { venue =>
        venueRepository.update(venue, venueData, User.fake).map {
          case success => Redirect(routes.VenueCtrl.get(id))
        }
      }
    )
  }
}
