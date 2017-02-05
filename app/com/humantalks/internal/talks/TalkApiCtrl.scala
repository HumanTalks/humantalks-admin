package com.humantalks.internal.talks

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.Controller

case class TalkApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    talkDbService: TalkDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find(q: Option[String] = None) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(talkDbService, q.map(TalkRepository.Filters.search).getOrElse(Json.obj())) }
  def get(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(talkDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(talkDbService, req.identity.id, req.body) }
  def update(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(talkDbService, req.identity.id, id, req.body) }
  def delete(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(talkDbService, id) }

  def duplicates(id: Option[String]) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    ApiHelper.duplicates[Talk](
      id,
      query => talkDbService.find(query),
      req.body,
      List("title"),
      elt => elt.data.title,
      elt => Json.toJson(elt.data),
      elt => routes.TalkCtrl.get(elt.id).toString
    )
  }
}
