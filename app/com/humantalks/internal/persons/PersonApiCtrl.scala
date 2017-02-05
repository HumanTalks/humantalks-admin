package com.humantalks.internal.persons

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.{ JsValue, JsObject, Json }
import play.api.mvc._

import scala.concurrent.Future

case class PersonApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find(q: Option[String] = None) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.find(personDbService, q.map(PersonRepository.Filters.search).getOrElse(Json.obj())) }
  def get(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.get(personDbService, id) }
  def create = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.create(personDbService, req.identity.id, req.body) }
  def update(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req => ApiHelper.update(personDbService, req.identity.id, id, req.body) }
  def delete(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req => ApiHelper.delete(personDbService, id) }

  def duplicates(id: Option[String]) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    ApiHelper.duplicates[Person](
      id,
      query => personDbService.find(query),
      req.body,
      List("name", "twitter", "email", "phone"),
      elt => elt.data.name,
      elt => Json.toJson(elt.data),
      elt => routes.PersonCtrl.get(elt.id).toString
    )
  }
}
