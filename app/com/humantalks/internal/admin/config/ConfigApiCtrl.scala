package com.humantalks.internal.admin.config

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.common.Conf
import com.humantalks.exposed.proposals.{ Proposal, ProposalDbService }
import com.humantalks.internal.meetups.{ Meetup, MeetupDbService }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.{ Talk, TalkDbService }
import com.humantalks.internal.venues.VenueDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import play.api.i18n.MessagesApi
import play.api.libs.json.{ JsValue, Json }
import play.api.mvc.{ RequestHeader, Result, Controller }

import scala.concurrent.Future
import scala.util.{ Try, Failure, Success }

case class ConfigApiCtrl(
    conf: Conf,
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService,
    proposalDbService: ProposalDbService,
    configDbService: ConfigDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def preview = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    (for {
      ref <- (req.body \ "ref").asOpt[String]
      value <- (req.body \ "value").asOpt[String]
    } yield ref match {
      case Config.meetupEventDescription.ref => for {
        meetupOpt <- meetupId(req.body).map(id => meetupDbService.get(id)).getOrElse(meetupDbService.getNext)
        venueOpt <- meetupOpt.flatMap(_.data.venue).map(venueDbService.get).getOrElse(Future.successful(None))
        talks <- meetupOpt.map(meetup => talkDbService.findByIds(meetup.data.talks)).getOrElse(Future.successful(List()))
        speakers <- personDbService.findByIds(talks.flatMap(_.data.speakers))
        res <- configDbService.buildMeetupEventDescription(meetupOpt, venueOpt, talks, speakers, _ => Future.successful(value))
      } yield buildResult(res)
      case Config.proposalSubmittedEmailSubject.ref => for {
        proposalOpt <- proposalId(req.body).map(id => proposalDbService.get(id)).getOrElse(proposalDbService.getLast)
        res <- configDbService.buildProposalSubmittedEmailSubject(proposalOpt, _ => Future.successful(value))
      } yield buildResult(res)
      case Config.proposalSubmittedEmailContent.ref => for {
        proposalOpt <- proposalId(req.body).map(id => proposalDbService.get(id)).getOrElse(proposalDbService.getLast)
        res <- configDbService.buildProposalSubmittedEmailContent(proposalOpt, proposalEditUrl(proposalOpt), conf.Organization.Admin.email, _ => Future.successful(value))
      } yield buildResult(res)
      case Config.proposalSubmittedSlackMessage.ref => for {
        proposalOpt <- proposalId(req.body).map(id => proposalDbService.get(id)).getOrElse(proposalDbService.getLast)
        speakers <- proposalOpt.map(p => personDbService.findByIds(p.data.speakers)).getOrElse(Future.successful(List()))
        res <- configDbService.buildProposalSubmittedSlackMessage(proposalOpt, speakers, proposalUrl(proposalOpt), _ => Future.successful(value))
      } yield buildResult(res)
      case Config.proposalSubmittedSlackTitle.ref => for {
        proposalOpt <- proposalId(req.body).map(id => proposalDbService.get(id)).getOrElse(proposalDbService.getLast)
        res <- configDbService.buildProposalSubmittedSlackTitle(proposalOpt, _ => Future.successful(value))
      } yield buildResult(res)
      case Config.proposalSubmittedSlackText.ref => for {
        proposalOpt <- proposalId(req.body).map(id => proposalDbService.get(id)).getOrElse(proposalDbService.getLast)
        res <- configDbService.buildProposalSubmittedSlackText(proposalOpt, _ => Future.successful(value))
      } yield buildResult(res)
      case Config.meetupCreatedSlackMessage.ref => for {
        meetupOpt <- meetupId(req.body).map(id => meetupDbService.get(id)).getOrElse(meetupDbService.getNext)
        res <- configDbService.buildMeetupCreatedSlackMessage(meetupOpt.map(_.data), meetupUrl(meetupOpt), _ => Future.successful(value))
      } yield buildResult(res)
      case Config.talkAddedToMeetupSlackMessage.ref => for {
        talkOpt <- talkId(req.body).map(id => talkDbService.get(id)).getOrElse(talkDbService.getLastAccepted)
        speakers <- talkOpt.map(t => personDbService.findByIds(t.data.speakers)).getOrElse(Future.successful(List()))
        meetupOpt <- talkOpt.map(t => meetupDbService.findForTalk(t.id).map(_.headOption)).getOrElse(Future.successful(None))
        addedByOpt <- personDbService.get(req.identity.id)
        res <- configDbService.buildTalkAddedToMeetupSlackMessage(talkOpt, speakers, meetupOpt, meetupUrl(meetupOpt), addedByOpt, _ => Future.successful(value))
      } yield buildResult(res)
      case _ => Future.successful(NotFound(Json.obj("error" -> s"No preview defined for ref '$ref'")))
    }).getOrElse(Future.successful(BadRequest(Json.obj("error" -> s"Incorrect payload format :\n\n${Json.prettyPrint(req.body)}\n\nAttributes 'ref' & 'value' are required !"))))
  }

  private def meetupId(json: JsValue): Option[Meetup.Id] = (json \ "id").asOpt[String].flatMap(id => Meetup.Id.from(id).right.toOption)
  private def talkId(json: JsValue): Option[Talk.Id] = (json \ "id").asOpt[String].flatMap(id => Talk.Id.from(id).right.toOption)
  private def proposalId(json: JsValue): Option[Proposal.Id] = (json \ "id").asOpt[String].flatMap(id => Proposal.Id.from(id).right.toOption)
  private def meetupUrl(meetupOpt: Option[Meetup])(implicit request: RequestHeader): String = meetupOpt.map(m => com.humantalks.internal.meetups.routes.MeetupCtrl.get(m.id).absoluteURL().toString).getOrElse("meetupUrl")
  private def proposalUrl(proposalOpt: Option[Proposal])(implicit request: RequestHeader): String = proposalOpt.map(p => com.humantalks.internal.proposals.routes.ProposalCtrl.get(p.id).absoluteURL().toString).getOrElse("proposalUrl")
  private def proposalEditUrl(proposalOpt: Option[Proposal])(implicit request: RequestHeader): String = proposalOpt.map(p => com.humantalks.exposed.proposals.routes.ProposalCtrl.update(p.id).absoluteURL().toString).getOrElse("proposalEditUrl")
  private def buildResult(res: (Try[String], Map[String, JsValue])): Result = res match {
    case (Success(result), scopes) => Ok(Json.obj("scopes" -> scopes, "result" -> result))
    case (Failure(e), scopes) => BadRequest(Json.obj("scopes" -> scopes, "error" -> e.getMessage))
  }
}
