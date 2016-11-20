package com.humantalks.internal.proposals

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.exposed.proposals.{ Proposal, ProposalDbService }
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.TalkDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.i18n.MessagesApi
import play.api.mvc.Controller
import scala.concurrent.Future

case class ProposalCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    proposalDbService: ProposalDbService,
    talkDbService: TalkDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      proposalList <- proposalDbService.find()
      personList <- personDbService.findByIds(proposalList.flatMap(_.data.speakers))
    } yield Ok(views.html.list(proposalList, personList))
  }

  def get(id: Proposal.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(proposalDbService.get _)(id) { proposal =>
      for {
        talkOpt <- proposal.talk.map(id => talkDbService.get(id)).getOrElse(Future.successful(None))
        speakers <- personDbService.findByIds(proposal.data.speakers)
      } yield Ok(views.html.detail(proposal, talkOpt, speakers))
    }
  }

  def setStatus(id: Proposal.Id, status: Proposal.Status.Value) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    proposalDbService.setStatus(id, status, req.identity.id).map { _ =>
      Redirect(CtrlHelper.getReferer(req.headers, routes.ProposalCtrl.get(id)))
    }
  }
}