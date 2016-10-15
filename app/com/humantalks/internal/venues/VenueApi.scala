package com.humantalks.internal.venues

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class VenueApi(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction.async { implicit req => ApiHelper.find(venueDbService) }
  def get(id: Venue.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.get(venueDbService, id) }
  def create = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.create(venueDbService, req.identity.id, req.body) }
  def update(id: Venue.Id) = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.update(venueDbService, req.identity.id, id, req.body) }
  def delete(id: Venue.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.delete(venueDbService, id) }
}
