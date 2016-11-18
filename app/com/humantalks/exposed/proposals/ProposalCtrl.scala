package com.humantalks.exposed.proposals

import com.humantalks.common.Conf
import com.humantalks.common.services.{ NotificationSrv, DateSrv }
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.TalkDbService
import global.Contexts
import global.helpers.CtrlHelper
import org.joda.time.{ DateTimeConstants, DateTime }
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{ Action, Controller, Result }

import scala.concurrent.Future

case class ProposalCtrl(
    conf: Conf,
    ctx: Contexts,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    proposalDbService: ProposalDbService,
    notificationSrv: NotificationSrv
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
        (_, id) <- proposalDbService.create(proposalData, proposalData.speakers.head)
        proposal <- proposalDbService.get(id).map(_.get)
        speakers <- personDbService.findByIds(proposal.data.speakers)
        success <- notificationSrv.proposalCreated(proposal, speakers)
      } yield Ok(views.html.submitted(proposal.id, proposalData))
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
        proposalDbService.update(proposal, proposalData, proposalData.speakers.head).map { _ =>
          Ok(views.html.submitted(id, proposalData))
        }
      }
    )
  }

  private def formView(status: Status, proposalForm: Form[Proposal.Data], proposalOpt: Option[Proposal]): Future[Result] = {
    personDbService.find().map { personList =>
      val now = new DateTime()
      val nextDates = (1 to 7).map(i => now.plusMonths(i)).map(d => DateSrv.getNthDayOfMonth(d, 2, DateTimeConstants.TUESDAY)).filter(_.toDateTimeAtCurrentTime.isAfterNow).toList
      status(views.html.form(proposalForm, proposalOpt, personList, nextDates, personForm))
    }
  }
}
