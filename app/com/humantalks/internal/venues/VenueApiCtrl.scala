package com.humantalks.internal.venues

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.Controller

case class VenueApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(venueDbService) }
  def get(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(venueDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(venueDbService, req.identity.id, req.body) }
  def update(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(venueDbService, req.identity.id, id, req.body) }
  def delete(id: Venue.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(venueDbService, id) }

  def duplicates(id: Option[String]) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    ApiHelper.duplicates[Venue](
      id,
      query => venueDbService.find(query),
      req.body,
      List("name", "twitter"),
      elt => elt.data.name,
      elt => Json.toJson(elt.data),
      elt => routes.VenueCtrl.get(elt.id).toString
    )
  }
}
