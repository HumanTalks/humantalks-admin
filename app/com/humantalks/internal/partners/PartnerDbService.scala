package com.humantalks.internal.partners

import com.humantalks.internal.events.{ Event, EventRepository }
import com.humantalks.internal.persons.Person
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class PartnerDbService(
    partnerRepository: PartnerRepository,
    eventRepository: EventRepository
) extends DbService[Partner, Partner.Id, Partner.Data, Person.Id] {
  val name = partnerRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = partnerRepository.defaultSort): Future[List[Partner]] = partnerRepository.find(filter, sort)
  def findByIds(ids: Seq[Partner.Id], sort: JsObject = partnerRepository.defaultSort): Future[List[Partner]] = partnerRepository.findByIds(ids, sort)
  def get(id: Partner.Id): Future[Option[Partner]] = partnerRepository.get(id)
  def create(elt: Partner.Data, by: Person.Id): Future[(WriteResult, Partner.Id)] = partnerRepository.create(elt, by)
  def update(elt: Partner, data: Partner.Data, by: Person.Id): Future[WriteResult] = partnerRepository.update(elt, data, by)
  def updateVenue(id: Partner.Id, venue: Partner.Venue, by: Person.Id): Future[WriteResult] = partnerRepository.updateVenue(id, venue, by)
  def addSponsor(id: Partner.Id, sponsor: Partner.Sponsor, by: Person.Id): Future[WriteResult] = partnerRepository.addSponsor(id, sponsor, by)
  def updateSponsor(id: Partner.Id, index: Int, sponsor: Partner.Sponsor, by: Person.Id): Future[WriteResult] = partnerRepository.updateSponsor(id, index, sponsor, by)
  def removeSponsor(id: Partner.Id, index: Int, by: Person.Id): Future[WriteResult] = partnerRepository.removeSponsor(id, index, by)
  def setMeetupRef(id: Partner.Id, meetupRef: Partner.MeetupRef, by: Person.Id): Future[WriteResult] = partnerRepository.setMeetupRef(id, meetupRef, by)

  def delete(id: Partner.Id): Future[Either[List[Event], WriteResult]] = {
    eventRepository.findForPartner(id).flatMap { events =>
      if (events.isEmpty) {
        partnerRepository.delete(id).map(r => Right(r))
      } else {
        Future.successful(Left(events))
      }
    }
  }
}
