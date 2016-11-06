package com.humantalks.common.services

import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import com.humantalks.common.services.slack.SlackSrv
import com.humantalks.exposed.proposals.Proposal
import com.humantalks.internal.meetups.{ MeetupDbService, Meetup }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ TalkDbService, Talk }
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }

case class NotificationSrv(conf: Conf, sendgridSrv: SendgridSrv, slackSrv: SlackSrv, personDbService: PersonDbService, talkDbService: TalkDbService, meetupDbService: MeetupDbService) {

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
        s"Nouvelle ${slackLink(url, "proposition de talk")} par ${speakers.map(toName).mkString(", ")} :",
        Some(Json.arr(Json.obj(
          "title" -> proposal.data.title,
          "text" -> proposal.data.description.getOrElse("").toString,
          "mrkdwn_in" -> Json.arr("pretext", "text")
        )))
      ).map(_.isRight)
    }
    Future.sequence(List(
      confirmationMail(proposal, speakers),
      slackMessage(proposal, speakers)
    )).map(_.forall(identity)).recover { case _: Throwable => false }
  }

  def meetupCreated(id: Meetup.Id, data: Meetup.Data)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    def slackMessage(id: Meetup.Id, data: Meetup.Data): Future[Boolean] = {
      val url = com.humantalks.internal.meetups.routes.MeetupCtrl.get(id).absoluteURL()
      slackSrv.postMessageAndCreateChannelIfNeeded(
        Meetup.slackChannel(data.date),
        s"Meetup ${slackLink(url, data.title)} créé !",
        None
      ).map(_.isRight)
    }
    Future.sequence(List(
      slackMessage(id, data)
    )).map(_.forall(identity)).recover { case _: Throwable => false }
  }

  def addTalkToMeetup(meetupId: Meetup.Id, talkId: Talk.Id, by: Person.Id)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    (for {
      personOpt <- personDbService.get(by)
      meetupOpt <- meetupDbService.get(meetupId)
      talkOpt <- talkDbService.get(talkId)
      speakers <- talkOpt.map(t => personDbService.findByIds(t.data.speakers)).getOrElse(Future.successful(List()))
    } yield {
      (for {
        person <- personOpt
        meetup <- meetupOpt
        talk <- talkOpt
      } yield addTalkToMeetup(meetup, talk, speakers, person)).getOrElse(Future.successful(false))
    }).flatMap(identity).recover { case _: Throwable => false }
  }
  def addTalkToMeetup(meetup: Meetup, talk: Talk, speakers: List[Person], by: Person)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    def slackMessage(meetup: Meetup, talk: Talk, speakers: List[Person], by: Person): Future[Boolean] = {
      val url = com.humantalks.internal.meetups.routes.MeetupCtrl.get(meetup.id).absoluteURL()
      slackSrv.postMessageAndCreateChannelIfNeeded(
        Meetup.slackChannel(meetup.data.date),
        s"Talk ajouté par ${by.data.name} pour les ${slackLink(url, meetup.data.title)} : *${talk.data.title}* par ${speakers.map(toName).mkString(", ")}",
        None
      ).map(_.isRight)
    }
    Future.sequence(List(
      slackMessage(meetup, talk, speakers, by)
    )).map(_.forall(identity)).recover { case _: Throwable => false }
  }

  private def toName(person: Person): String =
    person.data.email.map(email => slackMail(email, person.data.name)).getOrElse(person.data.name)
  private def slackLink(url: String, title: String = ""): String =
    if (title.length > 0) s"<$url|$title>" else url
  private def slackMail(mail: String, name: String = ""): String =
    if (name.length > 0) s"<mailto:$mail|$name>" else mail
}
