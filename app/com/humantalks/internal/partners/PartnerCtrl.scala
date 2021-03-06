package com.humantalks.internal.partners

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.events.EventDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import org.joda.time.LocalDate
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

case class PartnerCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    partnerDbService: PartnerDbService,
    personDbService: PersonDbService,
    eventDbService: EventDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val partnerForm = Form(Partner.fields)
  val venueForm = Form(Partner.venueFields)
  val sponsorForm = Form(Partner.sponsorFields)
  val personForm = Form(Person.fields)

  def find(filter: Option[String] = None, sort: Option[String] = None) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    val f = filter match {
      case Some("sponsor") => PartnerRepository.Filters.isSponsor(new LocalDate())
      case Some("venue") => PartnerRepository.Filters.isVenue
      case _ => Json.obj()
    }
    val s = sort match {
      case Some("date") => Json.obj("meta.created" -> 1)
      case Some("-date") => Json.obj("meta.created" -> -1)
      case Some("name") => Json.obj("data.name" -> 1)
      case Some("-name") => Json.obj("data.name" -> -1)
      case _ => PartnerRepository.defaultSort
    }
    partnerDbService.find(f, s).map { partnerList =>
      Ok(views.html.list(partnerList))
    }
  }

  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    formView(Ok, partnerForm, None)
  }

  def doCreate() = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    partnerForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      data => partnerDbService.create(data, req.identity.id).map {
        case (_, id) => Redirect(routes.PartnerCtrl.get(id))
      }
    )
  }

  def get(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(partnerDbService)(id) { partner =>
      for {
        eventList <- eventDbService.findForPartner(id)
        personList <- personDbService.findByIds(partner.data.allContacts)
      } yield Ok(views.html.detail(partner, eventList, personList))
    }
  }

  def update(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(partnerDbService)(id) { partner =>
      formView(Ok, partnerForm.fill(partner.data), Some(partner))
    }
  }

  def doUpdate(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    partnerForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(partnerDbService)(id) { partner => formView(BadRequest, formWithErrors, Some(partner)) },
      data => CtrlHelper.withItem(partnerDbService)(id) { partner =>
        partnerDbService.update(partner, data, req.identity.id).map { _ =>
          Redirect(routes.PartnerCtrl.get(id))
        }
      }
    )
  }

  def doDelete(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    partnerDbService.delete(id).map {
      case Left(events) => Redirect(routes.PartnerCtrl.get(id)).flashing("error" -> s"Unable to delete partner, it's still referenced in ${events.length} meetups, delete them first.")
      case Right(res) => Redirect(routes.PartnerCtrl.find()).flashing("success" -> "Partner deleted")
    }
  }

  def updateVenue(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(partnerDbService)(id) { partner =>
      venueFormView(Ok, partner.data.venue.map(v => venueForm.fill(v)).getOrElse(venueForm), partner.data.venue, partner)
    }
  }

  def doUpdateVenue(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    venueForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(partnerDbService)(id) { partner => venueFormView(BadRequest, formWithErrors, partner.data.venue, partner) },
      data => CtrlHelper.withItem(partnerDbService)(id) { partner =>
        partnerDbService.updateVenue(partner.id, data, req.identity.id).map { _ =>
          Redirect(routes.PartnerCtrl.get(id))
        }
      }
    )
  }

  def createSponsor(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(partnerDbService)(id) { partner =>
      sponsorFormView(Ok, sponsorForm, None, partner)
    }
  }

  def doCreateSponsor(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    sponsorForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(partnerDbService)(id) { partner => sponsorFormView(BadRequest, formWithErrors, None, partner) },
      data => partnerDbService.addSponsor(id, data, req.identity.id).map { _ =>
        Redirect(routes.PartnerCtrl.get(id))
      }
    )
  }

  def updateSponsor(id: Partner.Id, index: Int) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(partnerDbService)(id) { partner =>
      partner.data.sponsoring.lift(index).map { sponsor =>
        sponsorFormView(Ok, sponsorForm.fill(sponsor), Some(index), partner)
      }.getOrElse {
        Future.successful(Redirect(routes.PartnerCtrl.get(id)).flashing("error" -> s"Partner has not ${index + 1} sponsors"))
      }
    }
  }

  def doUpdateSponsor(id: Partner.Id, index: Int) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    sponsorForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(partnerDbService)(id) { partner => sponsorFormView(BadRequest, formWithErrors, Some(index), partner) },
      data => partnerDbService.updateSponsor(id, index, data, req.identity.id).map { _ =>
        Redirect(routes.PartnerCtrl.get(id))
      }
    )
  }

  def doDeleteSponsor(id: Partner.Id, index: Int) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    partnerDbService.removeSponsor(id, index, req.identity.id).map { _ =>
      Redirect(routes.PartnerCtrl.get(id))
    }
  }

  private def formView(status: Status, partnerForm: Form[Partner.Data], partnerOpt: Option[Partner])(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.form(partnerForm, partnerOpt, personForm)))
  }

  private def venueFormView(status: Status, venueForm: Form[Partner.Venue], venueOpt: Option[Partner.Venue], partner: Partner)(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.venueForm(venueForm, venueOpt, partner, personForm)))
  }

  private def sponsorFormView(status: Status, sponsorForm: Form[Partner.Sponsor], index: Option[Int], partner: Partner)(implicit request: RequestHeader, userOpt: Option[Person]): Future[Result] = {
    Future.successful(status(views.html.sponsorForm(sponsorForm, index, partner, personForm)))
  }
}
