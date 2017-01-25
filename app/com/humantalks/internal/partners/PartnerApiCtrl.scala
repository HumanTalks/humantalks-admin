package com.humantalks.internal.partners

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.Controller

case class PartnerApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    partnerDbService: PartnerDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(partnerDbService) }
  def get(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(partnerDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(partnerDbService, req.identity.id, req.body) }
  def update(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(partnerDbService, req.identity.id, id, req.body) }
  def delete(id: Partner.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(partnerDbService, id) }

  def duplicates(id: Option[String]) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    ApiHelper.duplicates[Partner](
      id,
      query => partnerDbService.find(query),
      req.body,
      List("name", "twitter"),
      elt => elt.data.name,
      elt => Json.toJson(elt.data),
      elt => routes.PartnerCtrl.get(elt.id).toString
    )
  }
}
