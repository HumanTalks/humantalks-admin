package com.humantalks.internal.venues

import com.humantalks.internal.meetups.{ Meetup, MeetupRepository }
import com.humantalks.internal.persons.Person
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class VenueDbService(venueRepository: VenueRepository, meetupRepository: MeetupRepository) extends DbService[Venue, Venue.Id, Venue.Data, Person.Id] {
  val name = venueRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.find(filter, sort)
  def findByIds(ids: Seq[Venue.Id], sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.findByIds(ids, sort)
  def get(id: Venue.Id): Future[Option[Venue]] = venueRepository.get(id)
  def create(elt: Venue.Data, by: Person.Id): Future[(WriteResult, Venue.Id)] = venueRepository.create(elt, by)
  def update(elt: Venue, data: Venue.Data, by: Person.Id): Future[WriteResult] = venueRepository.update(elt, data, by)
  def setMeetupRef(id: Venue.Id, meetupRef: Venue.MeetupRef, by: Person.Id): Future[WriteResult] = venueRepository.setMeetupRef(id, meetupRef, by)

  def delete(id: Venue.Id): Future[Either[List[Meetup], WriteResult]] = {
    meetupRepository.findForVenue(id).flatMap { meetups =>
      if (meetups.isEmpty) {
        venueRepository.delete(id).map(r => Right(r))
      } else {
        Future.successful(Left(meetups))
      }
    }
  }
}
