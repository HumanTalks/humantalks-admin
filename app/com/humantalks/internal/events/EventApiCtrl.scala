package com.humantalks.internal.events

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.Controller

case class EventApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    eventDbService: EventDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(eventDbService) }
  def get(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(eventDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(eventDbService, req.identity.id, req.body) }
  def update(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(eventDbService, req.identity.id, id, req.body) }
  def delete(id: Event.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(eventDbService, id) }

  def duplicates(id: Option[String]) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    ApiHelper.duplicates[Event](
      id,
      query => eventDbService.find(query),
      req.body,
      List("title", "date", "meetupUrl"),
      elt => elt.data.title,
      elt => Json.toJson(elt.data),
      elt => routes.EventCtrl.get(elt.id).toString
    )
  }
}
