package com.humantalks.common.services

import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import com.humantalks.common.services.slack.SlackSrv
import com.humantalks.internal.admin.config.ConfigDbService
import com.humantalks.internal.events.{ EventDbService, Event }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ TalkDbService, Talk }
import com.humantalks.internal.partners.Partner
import org.jsoup.Jsoup
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.RequestHeader

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

case class NotificationSrv(
    conf: Conf,
    sendgridSrv: SendgridSrv,
    slackSrv: SlackSrv,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    eventDbService: EventDbService,
    configDbService: ConfigDbService
) {

  def proposalCreated(talk: Talk, speakers: List[Person])(implicit request: RequestHeader, messageApi: MessagesApi, ec: ExecutionContext): Future[Boolean] = {
    def confirmationMail(talk: Talk, speakers: List[Person]): Future[Boolean] = {
      val url = com.humantalks.exposed.talks.routes.TalkCtrl.update(talk.id).absoluteURL()
      val admin = conf.Organization.Admin
      (for {
        (Success(subject), _) <- configDbService.buildProposalSubmittedEmailSubject(Some(talk))
        (Success(content), _) <- configDbService.buildProposalSubmittedEmailContent(Some(talk), url, admin.email)
        res <- sendgridSrv.send(Email(
          personalizations = Recipient.from(speakers),
          from = Address(admin.email, Some(admin.name)),
          subject = subject,
          content = Seq(
            Content.text(Jsoup.parse(content).text),
            Content.html(content)
          )
        ))
      } yield 200 <= res.status && res.status < 300).recover { case _ => false }
    }
    def slackMessage(talk: Talk, speakers: List[Person]): Future[Boolean] = {
      val url = com.humantalks.internal.talks.routes.TalkCtrl.get(talk.id).absoluteURL()
      (for {
        (Success(message), _) <- configDbService.buildProposalSubmittedSlackMessage(Some(talk), speakers, url)
        (Success(title), _) <- configDbService.buildProposalSubmittedSlackTitle(Some(talk))
        (Success(text), _) <- configDbService.buildProposalSubmittedSlackText(Some(talk))
        res <- slackSrv.postMessage(
          channel = conf.Slack.proposalChannel,
          text = message,
          attachments = Some(Json.arr(Json.obj("title" -> title, "text" -> text, "mrkdwn_in" -> Json.arr("pretext", "text"))))
        )
      } yield res.isRight).recover { case _ => false }
    }
    Future.sequence(List(
      confirmationMail(talk, speakers),
      slackMessage(talk, speakers)
    )).map(_.forall(identity)).recover { case _ => false }
  }

  def eventCreated(id: Event.Id, data: Event.Data)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    def slackMessage(id: Event.Id, event: Event.Data): Future[Boolean] = {
      val url = com.humantalks.internal.events.routes.EventCtrl.get(id).absoluteURL()
      (for {
        (Success(message), _) <- configDbService.buildMeetupCreatedSlackMessage(Some(event), url)
        res <- slackSrv.postMessageAndCreateChannelIfNeeded(
          channel = event.slackChannel,
          text = message,
          attachments = None
        )
      } yield res.isRight).recover { case _ => false }
    }
    Future.sequence(List(
      slackMessage(id, data)
    )).map(_.forall(identity)).recover { case _ => false }
  }

  def addTalkToEvent(eventId: Event.Id, talkId: Talk.Id, by: Person.Id)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    def slackMessage(event: Event, talk: Talk, speakers: List[Person], by: Person): Future[Boolean] = {
      val url = com.humantalks.internal.events.routes.EventCtrl.get(event.id).absoluteURL()
      (for {
        (Success(message), _) <- configDbService.buildTalkAddedToMeetupSlackMessage(Some(talk), speakers, Some(event), url, Some(by))
        res <- slackSrv.postMessageAndCreateChannelIfNeeded(
          channel = event.slackChannel,
          text = message,
          attachments = None
        )
      } yield res.isRight).recover { case _ => false }
    }
    def internal(event: Event, talk: Talk, speakers: List[Person], by: Person)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
      Future.sequence(List(
        slackMessage(event, talk, speakers, by)
      )).map(_.forall(identity)).recover { case _ => false }
    }
    (for {
      personOpt <- personDbService.get(by)
      eventOpt <- eventDbService.get(eventId)
      talkOpt <- talkDbService.get(talkId)
      speakers <- talkOpt.map(t => personDbService.findByIds(t.data.speakers)).getOrElse(Future.successful(List()))
    } yield {
      (for {
        person <- personOpt
        event <- eventOpt
        talk <- talkOpt
      } yield internal(event, talk, speakers, person)).getOrElse(Future.successful(false))
    }).flatMap(identity).recover { case _ => false }
  }

  def setVenueToEvent(eventId: Event.Id, partnerId: Partner.Id, by: Person.Id)(implicit request: RequestHeader, ec: ExecutionContext): Future[Boolean] = {
    Future.successful(true)
  }
}
