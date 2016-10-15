package com.humantalks.internal.persons

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc._

case class PersonApi(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction.async { implicit req => ApiHelper.find(personDbService) }
  def get(id: Person.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.get(personDbService, id) }
  def create = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.create(personDbService, req.identity.id, req.body) }
  def update(id: Person.Id) = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.update(personDbService, req.identity.id, id, req.body) }
  def delete(id: Person.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.delete(personDbService, id) }
}
