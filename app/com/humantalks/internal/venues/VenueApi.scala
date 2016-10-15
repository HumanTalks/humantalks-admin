package com.humantalks.internal.venues

import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class VenueApi(ctx: Contexts, venueRepository: VenueRepository) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = ApiHelper.findAction(venueRepository)
  def create = ApiHelper.createAction(venueRepository)
  def get(id: Venue.Id) = ApiHelper.getAction(venueRepository)(id)
  def update(id: Venue.Id) = ApiHelper.updateAction(venueRepository)(id)
  def delete(id: Venue.Id) = ApiHelper.deleteAction(venueRepository)(id)
}
