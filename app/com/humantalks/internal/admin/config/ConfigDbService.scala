package com.humantalks.internal.admin.config

import com.humantalks.common.services.MustacheSrv
import com.humantalks.exposed.entities.{ PublicPerson, PublicTalk, PublicVenue, PublicMeetup }
import com.humantalks.exposed.proposals.Proposal
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.infrastructure.DbService
import play.api.libs.json.{ JsValue, Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

case class ConfigDbService(configRepository: ConfigRepository) extends DbService[Config, Config.Id, Config.Data, Person.Id] {
  val name = configRepository.name
  def find(filter: JsObject = Json.obj(), sort: JsObject = configRepository.defaultSort): Future[List[Config]] = configRepository.find(filter, sort)
  def get(id: Config.Id): Future[Option[Config]] = configRepository.get(id)
  def getByRef(ref: String): Future[Option[Config]] = configRepository.getByRef(ref)
  def create(elt: Config.Data, by: Person.Id): Future[(WriteResult, Config.Id)] = configRepository.create(elt, by)
  def update(elt: Config, data: Config.Data, by: Person.Id): Future[WriteResult] = configRepository.update(elt, data, by)
  def setValue(id: Config.Id, value: String, by: Person.Id): Future[WriteResult] = configRepository.setValue(id, value, by)
  def delete(id: Config.Id): Future[Either[Nothing, WriteResult]] = configRepository.delete(id).map(res => Right(res))

  def buildMeetupEventDescription(meetupOpt: Option[Meetup], venueOpt: Option[Venue], talks: List[Talk], speakers: List[Person], getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.meetupEventDescription).map(tempate => build(tempate, Map(
      "meetup" -> Json.toJson(meetupOpt.map(m => PublicMeetup.from(m, None, None, None))),
      "venue" -> Json.toJson(venueOpt.map(v => PublicVenue.from(v, None, None, None))),
      "talks" -> Json.toJson(talks.map(t => PublicTalk.from(t, Some(speakers), None, None)))
    )))
  def buildProposalSubmittedEmailSubject(proposalOpt: Option[Proposal], getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.proposalSubmittedEmailSubject).map(tempate => build(tempate, Map(
      "proposal" -> Json.toJson(proposalOpt)
    )))
  def buildProposalSubmittedEmailContent(proposalOpt: Option[Proposal], proposalEditUrl: String, emailOrga: String, getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.proposalSubmittedEmailContent).map(tempate => build(tempate, Map(
      "proposal" -> Json.toJson(proposalOpt),
      "proposalEditUrl" -> Json.toJson(proposalEditUrl),
      "emailOrga" -> Json.toJson(emailOrga)
    )))
  def buildProposalSubmittedSlackMessage(proposalOpt: Option[Proposal], speakers: List[Person], proposalUrl: String, getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.proposalSubmittedSlackMessage).map(tempate => build(tempate, Map(
      "proposal" -> Json.toJson(proposalOpt),
      "speakers" -> Json.toJson(speakers.map(s => PublicPerson.from(s, None, None, None))),
      "proposalUrl" -> Json.toJson(proposalUrl)
    )))
  def buildProposalSubmittedSlackTitle(proposalOpt: Option[Proposal], getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.proposalSubmittedSlackTitle).map(tempate => build(tempate, Map(
      "proposal" -> Json.toJson(proposalOpt)
    )))
  def buildProposalSubmittedSlackText(proposalOpt: Option[Proposal], getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.proposalSubmittedSlackText).map(tempate => build(tempate, Map(
      "proposal" -> Json.toJson(proposalOpt)
    )))
  def buildMeetupCreatedSlackMessage(meetup: Option[Meetup.Data], meetupUrl: String, getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.meetupCreatedSlackMessage).map(tempate => build(tempate, Map(
      "meetup" -> Json.toJson(meetup),
      "meetupUrl" -> Json.toJson(meetupUrl)
    )))
  def buildTalkAddedToMeetupSlackMessage(talkOpt: Option[Talk], speakers: List[Person], meetupOpt: Option[Meetup], meetupUrl: String, addedByOpt: Option[Person], getTemplate: Config.Data => Future[String] = getValue): Future[(Try[String], Map[String, JsValue])] =
    getTemplate(Config.talkAddedToMeetupSlackMessage).map(tempate => build(tempate, Map(
      "talk" -> Json.toJson(talkOpt.map(talk => PublicTalk.from(talk, Some(speakers), None, None))),
      "meetup" -> Json.toJson(meetupOpt),
      "meetupUrl" -> Json.toJson(meetupUrl),
      "addedBy" -> Json.toJson(addedByOpt)
    )))
  private def build(template: String, scopes: Map[String, JsValue]): (Try[String], Map[String, JsValue]) =
    (MustacheSrv.build(template, scopes), scopes)
  private def getValue(data: Config.Data): Future[String] =
    configRepository.getByRef(data.ref).map(_.map(_.data.value).getOrElse(data.value))
}
