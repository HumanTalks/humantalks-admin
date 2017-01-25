package com.humantalks.internal.events

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.common.services.NotificationSrv
import com.humantalks.common.services.meetup.MeetupSrv
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ TalkDbService, Talk }
import com.humantalks.internal.partners.{ Partner, PartnerDbService }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import org.joda.time.DateTime
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class EventCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    partnerDbService: PartnerDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    eventDbService: EventDbService,
    meetupSrv: MeetupSrv,
    notificationSrv: NotificationSrv
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val eventForm = Form(Event.fields)
  val partnerForm = Form(Partner.fields)
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      eventList <- eventDbService.find()
      partnerList <- partnerDbService.findByIds(eventList.flatMap(_.data.venue))
    } yield Ok(views.html.list(eventList, partnerList))
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    eventDbService.getLast.flatMap { eventOpt =>
      val lastDate = eventOpt.map(_.data.date).getOrElse(new DateTime())
      formView(Ok, eventForm.fill(Event.Data.generate(lastDate)), None)
    }
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    eventForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      eventData => eventDbService.create(eventData, req.identity.id).map {
        case (_, id) =>
          notificationSrv.eventCreated(id, eventData)
          Redirect(routes.EventCtrl.get(id))
      }
    )
  }

  def get(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        eventPartners <- partnerDbService.findByIds(event.data.venue.toSeq)
        eventTalks <- talkDbService.findByIds(event.data.talks)
        eventSpeakers <- personDbService.findByIds(eventTalks.flatMap(_.data.speakers))
        pendingTalks <- talkDbService.findPending()
        pendingSpeakers <- personDbService.findByIds(pendingTalks.flatMap(_.data.speakers))
      } yield Ok(views.html.detail(event, partnerForm, personForm, talkForm, eventPartners, eventTalks, eventSpeakers, pendingTalks, pendingSpeakers))
    }
  }

  def update(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(eventDbService)(id) { event =>
      formView(Ok, eventForm.fill(event.data), Some(event))
    }
  }

  def doUpdate(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    eventForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(eventDbService)(id) { event => formView(BadRequest, formWithErrors, Some(event)) },
      eventData => CtrlHelper.withItem(eventDbService)(id) { event =>
        eventDbService.update(event, eventData, req.identity.id).map {
          case _ => Redirect(routes.EventCtrl.get(id))
        }
      }
    )
  }

  def doCreatePartner(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    partnerForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(redirectUrl).flashing("error" -> "Incorrect form values")),
      data => partnerDbService.create(data, req.identity.id).flatMap {
        case (_, partnerId) =>
          eventDbService.setVenue(id, partnerId, req.identity.id).map { _ =>
            notificationSrv.setVenueToEvent(id, partnerId, req.identity.id)
            Redirect(redirectUrl)
          }
      }
    )
  }

  def doAddPartnerForm(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.getFormParam(req.body, "partnerId").flatMap(p => Partner.Id.from(p).right.toOption).map { partnerId =>
      eventDbService.setVenue(id, partnerId, req.identity.id).map { _ =>
        notificationSrv.setVenueToEvent(id, partnerId, req.identity.id)
        Redirect(redirectUrl)
      }
    }.getOrElse {
      Future.successful(Redirect(redirectUrl).flashing("error" -> "Unable to find partner :("))
    }
  }

  def doCreateTalk(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    talkForm.bindFromRequest.fold(
      formWithErrors => Future.successful(Redirect(redirectUrl).flashing("error" -> "Incorrect form values")),
      talkData => talkDbService.create(talkData, req.identity.id).flatMap {
        case (_, talkId) =>
          eventDbService.addTalk(id, talkId, req.identity.id).map { _ =>
            notificationSrv.addTalkToEvent(id, talkId, req.identity.id)
            Redirect(redirectUrl)
          }
      }
    )
  }

  def doAddTalkForm(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.getFormParam(req.body, "talkId").flatMap(p => Talk.Id.from(p).right.toOption).map { talkId =>
      eventDbService.addTalk(id, talkId, req.identity.id).map { _ =>
        notificationSrv.addTalkToEvent(id, talkId, req.identity.id)
        Redirect(redirectUrl)
      }
    }.getOrElse {
      Future.successful(Redirect(redirectUrl).flashing("error" -> "Unable to find talk :("))
    }
  }

  def doAddTalk(id: Event.Id, talkId: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    eventDbService.addTalk(id, talkId, req.identity.id).map { _ =>
      notificationSrv.addTalkToEvent(id, talkId, req.identity.id)
      Redirect(CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id)))
    }
  }

  def doMoveTalk(id: Event.Id, talkId: Talk.Id, up: Boolean) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    eventDbService.moveTalk(id, talkId, up, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id)))
    }
  }

  def doRemoveTalk(id: Event.Id, talkId: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    eventDbService.removeTalk(id, talkId, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id)))
    }
  }

  def doPublish(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        partnerOpt <- event.data.venue.map(id => partnerDbService.get(id)).getOrElse(Future.successful(None))
        talkList <- talkDbService.findByIds(event.data.talks)
        personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
        res <- meetupSrv.create(event, partnerOpt, talkList, personList, req.identity.id)
      } yield res match {
        case Right(_) => Redirect(redirectUrl)
        case Left(errs) => Redirect(redirectUrl).flashing("error" -> errs.mkString(", "))
      }
    }
  }

  def doUpdatePublish(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        partnerOpt <- event.data.venue.map(id => partnerDbService.get(id)).getOrElse(Future.successful(None))
        talkList <- talkDbService.findByIds(event.data.talks)
        personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
        res <- meetupSrv.update(event, partnerOpt, talkList, personList, req.identity.id)
      } yield res match {
        case Right(_) => Redirect(redirectUrl)
        case Left(errs) => Redirect(redirectUrl).flashing("error" -> errs.mkString(", "))
      }
    }
  }

  def doAnnounce(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        res <- meetupSrv.announce(event, req.identity.id)
      } yield res match {
        case Right(_) => Redirect(redirectUrl)
        case Left(errs) => Redirect(redirectUrl).flashing("error" -> errs.mkString(", "))
      }
    }
  }

  def doUnpublish(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    val redirectUrl = CtrlHelper.getReferer(req.headers, routes.EventCtrl.get(id))
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        res <- meetupSrv.delete(event, req.identity.id)
      } yield res match {
        case Right(_) => Redirect(redirectUrl)
        case Left(errs) => Redirect(redirectUrl).flashing("error" -> errs.mkString(", "))
      }
    }
  }

  def doDelete(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    eventDbService.delete(id).map {
      case Left(nothing) => Redirect(routes.EventCtrl.get(id)).flashing("error" -> "Unable to delete meetup (should NEVER happen)")
      case Right(res) => Redirect(routes.EventCtrl.find()).flashing("success" -> "Meetup deleted")
    }
  }

  private def formView(status: Status, eventForm: Form[Event.Data], eventOpt: Option[Event])(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(eventForm, talkForm, personForm, eventOpt)))
  }
}
