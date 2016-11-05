package com.humantalks.common.services

import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import com.humantalks.common.services.slack.SlackSrv
import com.humantalks.exposed.proposals.Proposal
import com.humantalks.internal.persons.Person
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }

case class NotificationSrv(conf: Conf, sendgridSrv: SendgridSrv, slackSrv: SlackSrv) {

  def proposalCreated(proposal: Proposal, speakers: List[Person])(implicit request: RequestHeader, messageApi: MessagesApi, ec: ExecutionContext): Future[Boolean] = {
    def confirmationMail(proposal: Proposal, speakers: List[Person]): Future[Boolean] = {
      val url = com.humantalks.exposed.proposals.routes.ProposalCtrl.update(proposal.id).absoluteURL()
      sendgridSrv.send(Email(
        personalizations = Seq(Recipient(speakers.flatMap(s => s.data.email.map(email => Address(email, Some(s.data.name)))))),
        from = Address(conf.Organization.Admin.email, Some(conf.Organization.Admin.name)),
        subject = "Thanks for submitting to HumanTalks Paris",
        content = Seq(
          Content.text(com.humantalks.exposed.proposals.views.txt.emails.proposalSubmited(proposal, url, conf.Organization.Admin.email).body),
          Content.html(com.humantalks.exposed.proposals.views.html.emails.proposalSubmited(proposal, url, conf.Organization.Admin.email).body)
        )
      )).map(res => 200 <= res.status && res.status < 300).recover {
        case _ => false
      }
    }
    def slackMessage(proposal: Proposal, speakers: List[Person]): Future[Boolean] = {
      val url = com.humantalks.internal.proposals.routes.ProposalCtrl.get(proposal.id).absoluteURL()
      slackSrv.postMessage(
        conf.Slack.proposalChannel,
        s"Nouvelle proposition de talk : $url",
        Some(Json.arr(Json.obj(
          "title" -> proposal.data.title,
          "pretext" -> ("Par " + speakers.map(p => p.data.name + p.data.email.map(e => s"<$e>").getOrElse("")).mkString(", ")),
          "text" -> proposal.data.description.getOrElse("").toString,
          "mrkdwn_in" -> Json.arr("pretext", "text")
        )))
      )
    }
    Future.sequence(List(
      confirmationMail(proposal, speakers),
      slackMessage(proposal, speakers)
    )).map(_.forall(identity))
  }
}
