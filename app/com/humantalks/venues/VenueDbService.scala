package com.humantalks.venues

import com.humantalks.auth.models.User
import global.infrastructure.DbService
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class VenueDbService(venueRepository: VenueRepository) extends DbService[Venue, Venue.Id] {
  val name = venueRepository.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = venueRepository.defaultSort): Future[List[Venue]] = venueRepository.find(filter, sort)
  def get(id: Venue.Id): Future[Option[Venue]] = venueRepository.get(id)
  def create(elt: Venue.Data, by: User.Id): Future[(WriteResult, Venue.Id)] = venueRepository.create(elt, by)
  def update(elt: Venue, data: Venue.Data, by: User.Id): Future[WriteResult] = venueRepository.update(elt, data, by)
}
