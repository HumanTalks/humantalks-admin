package com.humantalks.meetups

import com.humantalks.common.models.User
import com.humantalks.venues.VenueRepository
import com.humantalks.meetups.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(ctx: Contexts, meetupRepository: MeetupRepository, venueRepository: VenueRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    meetupRepository.find().map { meetupList =>
      Ok(html.list(meetupList))
    }
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, meetupForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => {
        meetupRepository.create(meetupData, User.fake).map {
          case (success, id) => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def get(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    withMeetup(id) { meetup =>
      Future(Ok(html.detail(meetup)))
    }
  }

  def update(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    withMeetup(id) { meetup =>
      formView(Ok, meetupForm.fill(meetup.data), Some(meetup))
    }
  }

  def doUpdate(id: Meetup.Id) = Action.async { implicit req: Request[AnyContent] =>
    meetupForm.bindFromRequest.fold(
      formWithErrors => withMeetup(id) { meetup => formView(BadRequest, formWithErrors, Some(meetup)) },
      meetupData => {
        meetupRepository.get(id).flatMap { meetupOpt =>
          meetupOpt.map { meetup =>
            meetupRepository.update(meetup.copy(data = meetupData), User.fake).map {
              case success => Redirect(routes.MeetupCtrl.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def formView(status: Status, meetupForm: Form[Meetup.Data], meetupOpt: Option[Meetup]): Future[Result] = {
    venueRepository.find().map { venueList =>
      status(html.form(meetupForm, meetupOpt, venueList))
    }
  }
  private def notFound(id: Meetup.Id): Result =
    NotFound(com.humantalks.common.views.html.errors.notFound("Unable to find a Meetup with id " + id))
  private def withMeetup(id: Meetup.Id)(block: Meetup => Future[Result]): Future[Result] = {
    meetupRepository.get(id).flatMap { meetupOpt =>
      meetupOpt.map { meetup =>
        block(meetup)
      }.getOrElse {
        Future(notFound(id))
      }
    }
  }
}
