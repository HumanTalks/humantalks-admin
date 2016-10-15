package com.humantalks.internal.venues

import com.humantalks.auth.entities.User
import com.humantalks.internal.meetups.{ Meetup, MeetupRepository }
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

case class VenueDbService(meetupRepository: MeetupRepository, venueRepository: VenueRepository) extends DbService[Venue, Venue.Id, Venue.Data, User.Id] {
  val name = venueRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.find(filter, sort)
  def findByIds(ids: Seq[Venue.Id], sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.findByIds(ids, sort)
  def get(id: Venue.Id): Future[Option[Venue]] = venueRepository.get(id)
  def create(elt: Venue.Data, by: User.Id): Future[(WriteResult, Venue.Id)] = venueRepository.create(elt, by)
  def update(elt: Venue, data: Venue.Data, by: User.Id): Future[WriteResult] = venueRepository.update(elt, data, by)

  def delete(id: Venue.Id)(implicit ec: ExecutionContext): Future[Either[List[Meetup], WriteResult]] = {
    meetupRepository.findForVenue(id).flatMap { meetups =>
      if (meetups.isEmpty) {
        venueRepository.delete(id).map(r => Right(r))
      } else {
        Future(Left(meetups))
      }
    }
  }
}
