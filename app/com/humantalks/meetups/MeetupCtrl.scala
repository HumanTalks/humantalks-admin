package com.humantalks.meetups

import com.humantalks.common.models.User
import com.humantalks.talks.TalkRepository
import com.humantalks.venues.VenueRepository
import com.humantalks.meetups.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(ctx: Contexts, meetupRepository: MeetupRepository, venueRepository: VenueRepository, talkRepository: TalkRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      meetupList <- meetupRepository.find()
      venueMap <- venueRepository.findByIds(meetupList.map(_.data.venue).flatten).map(_.map(v => (v.id, v)).toMap)
    } yield Ok(html.list(meetupList, venueMap))
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
      val venueListFut = venueRepository.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkRepository.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
      } yield Ok(html.detail(meetup, venueList.map(v => (v.id, v)).toMap, talkList.map(t => (t.id, t)).toMap))
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
    val venueListFut = venueRepository.find()
    val talkListFut = talkRepository.find()
    for {
      venueList <- venueListFut
      talkList <- talkListFut
    } yield status(html.form(meetupForm, meetupOpt, venueList, talkList))
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
