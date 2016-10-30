package com.humantalks.internal.meetups

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.exposed.proposals.{ Proposal, ProposalDbService }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ TalkDbService, Talk }
import com.humantalks.internal.venues.VenueDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import org.joda.time.{ LocalTime, DateTimeConstants, DateTime }
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class MeetupCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService,
    proposalDbService: ProposalDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val meetupForm = Form(Meetup.fields)
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      meetupList <- meetupDbService.find()
      venueList <- venueDbService.findByIds(meetupList.flatMap(_.data.venue))
    } yield Ok(views.html.list(meetupList, venueList))
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    meetupDbService.getLast.flatMap { meetupOpt =>
      val nextDate = Meetup.nextDate(meetupOpt.map(_.data.date).getOrElse(new DateTime()), 2, DateTimeConstants.TUESDAY, new LocalTime(19, 0))
      val nextTitle = Meetup.title(nextDate)
      val nextMeetup = Meetup.Data(nextTitle, nextDate, None, List(), None, None, None)
      formView(Ok, meetupForm.fill(nextMeetup), None)
    }
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    meetupForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      meetupData => meetupDbService.create(meetupData, req.identity.id).map {
        case (_, id) => Redirect(routes.MeetupCtrl.get(id))
      }
    )
  }

  def get(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueDbService.findByIds(meetup.data.venue.toSeq)
      val allTalksFut = talkDbService.find()
      val pendingTalksFut = talkDbService.findPending()
      val allPersonsFut = personDbService.find()
      val pendingProposalsFut = proposalDbService.findPending()
      for {
        venueList <- venueListFut
        allTalks <- allTalksFut
        pendingTalks <- pendingTalksFut
        allPersons <- allPersonsFut
        pendingProposals <- pendingProposalsFut
      } yield Ok(views.html.detail(meetup, talkForm, personForm, pendingTalks, allTalks, allPersons, venueList, pendingProposals))
    }
  }

  def update(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      formView(Ok, meetupForm.fill(meetup.data), Some(meetup))
    }
  }

  def doUpdate(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    meetupForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(meetupDbService)(id) { meetup => formView(BadRequest, formWithErrors, Some(meetup)) },
      meetupData => CtrlHelper.withItem(meetupDbService)(id) { meetup =>
        meetupDbService.update(meetup, meetupData, req.identity.id).map {
          case _ => Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doCreateTalk(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(routes.MeetupCtrl.get(id)).flashing("error" -> "Incorrect form values")),
      talkData => talkDbService.create(talkData, req.identity.id).flatMap {
        case (_, talkId) => meetupDbService.addTalk(id, talkId, req.identity.id).map { _ =>
          Redirect(routes.MeetupCtrl.get(id))
        }
      }
    )
  }

  def doAddTalkForm(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    req.body.asFormUrlEncoded.get("talkId").headOption.flatMap(p => Talk.Id.from(p).right.toOption).map { talkId =>
      meetupDbService.addTalk(id, talkId, req.identity.id).map { _ =>
        Redirect(routes.MeetupCtrl.get(id))
      }
    }.getOrElse {
      Future.successful(Redirect(routes.MeetupCtrl.get(id)).flashing("error" -> "Unable to find talk :("))
    }
  }

  def doAddTalk(id: Meetup.Id, talkId: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    meetupDbService.addTalk(id, talkId, req.identity.id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doMoveTalk(id: Meetup.Id, talkId: Talk.Id, up: Boolean) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    meetupDbService.moveTalk(id, talkId, up, req.identity.id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doRemoveTalk(id: Meetup.Id, talkId: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    meetupDbService.removeTalk(id, talkId, req.identity.id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doAddProposal(id: Meetup.Id, proposalId: Proposal.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    proposalDbService.accept(proposalId, req.identity.id).flatMap {
      case Right(talkId) => meetupDbService.addTalk(id, talkId, req.identity.id).map { _ =>
        Redirect(routes.MeetupCtrl.get(id)).flashing("success" -> "Proposal transformed into a talk and added to meetup :)")
      }
      case Left(err) => Future.successful(Redirect(routes.MeetupCtrl.get(id)).flashing("error" -> err))
    }
  }

  def publish(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(meetupDbService)(id) { meetup =>
      val venueListFut = venueDbService.findByIds(meetup.data.venue.toSeq)
      val talkListFut = talkDbService.findByIds(meetup.data.talks)
      for {
        venueList <- venueListFut
        talkList <- talkListFut
        personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
      } yield Ok(views.html.publish(meetup, talkList, personList, venueList))
    }
  }

  def doPublish(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    meetupDbService.setPublished(id, req.identity.id).map { _ =>
      Redirect(routes.MeetupCtrl.get(id))
    }
  }

  def doDelete(id: Meetup.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    meetupDbService.delete(id).map {
      _ match {
        case Left(nothing) => Redirect(routes.MeetupCtrl.get(id)).flashing("error" -> "Unable to delete meetup (should NEVER happen)")
        case Right(res) => Redirect(routes.MeetupCtrl.find()).flashing("success" -> "Meetup deleted")
      }
    }
  }

  private def formView(status: Status, meetupForm: Form[Meetup.Data], meetupOpt: Option[Meetup])(implicit request: RequestHeader, user: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(meetupForm, talkForm, personForm, meetupOpt)))
  }
}
