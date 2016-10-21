package com.humantalks.internal.proposals

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.exposed.proposals.{ Proposal, ProposalDbService }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.CtrlHelper
import play.api.i18n.MessagesApi
import play.api.mvc.Controller

case class ProposalCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    proposalDbService: ProposalDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def get(id: Proposal.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    implicit val user = Some(req.identity)
    CtrlHelper.withItem(proposalDbService.get _)(id) { proposal =>
      personDbService.findByIds(proposal.data.speakers).map { speakers =>
        Ok(views.html.detail(proposal, speakers))
      }
    }
  }
}