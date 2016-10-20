package com.humantalks.exposed.proposals

import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import com.humantalks.internal.persons.{ Person, PersonDbService }
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSResponse
import play.api.mvc.{ RequestHeader, Action, Controller, Result }

import scala.concurrent.Future

case class ProposalCtrl(
    conf: Conf,
    ctx: Contexts,
    personDbService: PersonDbService,
    proposalDbService: ProposalDbService,
    sendgridSrv: SendgridSrv
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._
  val proposalForm = Form(Proposal.fields)
  val personForm = Form(Person.fields)

  def create() = Action.async { implicit req =>
    formView(Ok, proposalForm, None)
  }

  def doCreate() = Action.async { implicit req =>
    proposalForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      proposalData => for {
        (_, proposal) <- proposalDbService.create(proposalData)
        personOpt <- personDbService.get(proposal.data.speakers.head)
        success <- sendSubmitedMail(proposal, personOpt.get)
      } yield Ok(views.html.submited(proposal.id, proposalData))
    )
  }

  def update(id: Proposal.Id) = Action.async { implicit req =>
    CtrlHelper.withItem(proposalDbService.get _)(id) { proposal =>
      formView(Ok, proposalForm.fill(proposal.data), Some(proposal))
    }
  }

  def doUpdate(id: Proposal.Id) = Action.async { implicit req =>
    proposalForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(proposalDbService.get _)(id) { proposal => formView(BadRequest, formWithErrors, Some(proposal)) },
      proposalData => CtrlHelper.withItem(proposalDbService.get _)(id) { proposal =>
        proposalDbService.update(proposal, proposalData).map {
          case _ => Ok(views.html.submited(id, proposalData))
        }
      }
    )
  }

  private def formView(status: Status, proposalForm: Form[Proposal.Data], proposalOpt: Option[Proposal]): Future[Result] = {
    personDbService.find().map { personList =>
      status(views.html.form(proposalForm, proposalOpt, personList, personForm))
    }
  }
  private def sendSubmitedMail(proposal: Proposal, person: Person)(implicit request: RequestHeader): Future[Boolean] = {
    val url = routes.ProposalCtrl.update(proposal.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(person.data.email.get, Some(person.data.name)),
      from = Address(conf.Organization.Admin.email, Some(conf.Organization.Admin.name)),
      subject = "Thanks for submitting to HumanTalks Paris",
      content = Seq(
        Content.text(views.txt.emails.proposalSubmited(proposal, person, url, conf.Organization.Admin.email).body),
        Content.html(views.html.emails.proposalSubmited(proposal, person, url, conf.Organization.Admin.email).body)
      )
    )).map(res => 200 <= res.status && res.status < 300).recover {
      case _ => false
    }
  }
}
