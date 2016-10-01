package com.humantalks.controllers.html

import com.humantalks.domain.{ Venue, User, VenueData }
import com.humantalks.infrastructure.VenueRepository
import com.humantalks.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class Venues(ctx: Contexts, venueRepository: VenueRepository)(implicit messageApi: MessagesApi) extends Controller {
  import ctx._
  import Contexts.ctrlToEC
  val venueForm = Form(VenueData.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    venueRepository.find().map { venueList =>
      Ok(html.venues.list(venueList))
    }
  }

  def create = Action { implicit req: Request[AnyContent] =>
    Ok(html.venues.form(venueForm, None))
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(html.venues.form(formWithErrors, None))),
      venueData => {
        venueRepository.create(venueData, User.fake).map {
          case (success, id) => Redirect(routes.Venues.get(id))
        }
      }
    )
  }

  def get(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    withVenue(id) { venue =>
      Ok(html.venues.detail(venue))
    }
  }

  def update(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    withVenue(id) { venue =>
      Ok(html.venues.form(venueForm.fill(venue.data), Some(venue)))
    }
  }

  def doUpdate(id: Venue.Id) = Action.async { implicit req: Request[AnyContent] =>
    venueForm.bindFromRequest.fold(
      formWithErrors => withVenue(id) { venue => BadRequest(html.venues.form(formWithErrors, Some(venue))) },
      venueData => {
        venueRepository.get(id).flatMap { venueOpt =>
          venueOpt.map { venue =>
            venueRepository.update(venue.copy(data = venueData), User.fake).map {
              case success => Redirect(routes.Venues.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def notFound(id: Venue.Id): Result =
    NotFound(html.errors.notFound("Unable to find a Venue with id " + id))
  private def withVenue(id: Venue.Id)(block: Venue => Result): Future[Result] = {
    venueRepository.get(id).map { venueOpt =>
      venueOpt.map { venue =>
        block(venue)
      }.getOrElse {
        notFound(id)
      }
    }
  }
}
