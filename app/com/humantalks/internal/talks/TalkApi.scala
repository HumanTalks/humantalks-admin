package com.humantalks.internal.talks

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class TalkApi(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    talkDbService: TalkDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(talkDbService) }
  def get(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(talkDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(talkDbService, req.identity.id, req.body) }
  def update(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(talkDbService, req.identity.id, id, req.body) }
  def delete(id: Talk.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(talkDbService, id) }
}
