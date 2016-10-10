package com.humantalks.venues

import com.humantalks.auth.silhouette.User
import com.humantalks.meetups.{ MeetupRepository, Meetup }
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.{ ExecutionContext, Future }

case class VenueDbService(meetupRepository: MeetupRepository, venueRepository: VenueRepository) extends DbService[Venue, Venue.Id] {
  val name = venueRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.find(filter, sort)
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
