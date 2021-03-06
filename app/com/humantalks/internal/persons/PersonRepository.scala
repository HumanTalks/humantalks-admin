package com.humantalks.internal.persons

import com.humantalks.auth.forms.RegisterForm
import com.humantalks.common.Conf
import com.humantalks.internal.persons.Person.Role
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class PersonRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Person, Person.Id, Person.Data, Person.Id] with IdentityService[Person] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.person)
  val name: String = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = PersonRepository.defaultSort): Future[List[Person]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = PersonRepository.defaultSort): Future[Page[Person]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Person.Id], sort: JsObject = PersonRepository.defaultSort): Future[List[Person]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def get(id: Person.Id): Future[Option[Person]] =
    collection.get(Json.obj("id" -> id))

  def getByEmail(email: String): Future[Option[Person]] =
    collection.get(Json.obj("data.email" -> email))

  def create(elt: Person): Future[(WriteResult, Person.Id)] =
    collection.create(elt).map { res => (res, elt.id) }

  def create(data: Person.Data, by: Person.Id): Future[(WriteResult, Person.Id)] =
    create(Person.from(data.trim, by))

  def update(elt: Person): Future[WriteResult] =
    collection.update(Json.obj("id" -> elt.id), elt)

  def update(elt: Person, data: Person.Data, by: Person.Id): Future[WriteResult] =
    update(elt.copy(data = data.trim, meta = elt.meta.update(by)))

  private def partialUpdate(id: Person.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def delete(id: Person.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))

  /* Method used for Auth */

  def findUsers(filter: JsObject = Json.obj(), sort: JsObject = PersonRepository.defaultSort): Future[List[Person]] =
    collection.find(Json.obj("auth" -> Json.obj("$exists" -> true)) ++ filter, sort)

  def retrieve(loginInfo: LoginInfo): Future[Option[Person]] =
    collection.get(Json.obj("auth.loginInfo.providerID" -> loginInfo.providerID, "auth.loginInfo.providerKey" -> loginInfo.providerKey))

  def register(form: RegisterForm, loginInfo: LoginInfo, avatar: Option[String] = None): Future[Person] = {
    getByEmail(form.email).flatMap { personOpt =>
      personOpt.map { person =>
        val p = person.copy(
          data = person.data.copy(
            name = form.name,
            avatar = avatar.orElse(person.data.avatar)
          ),
          auth = Some(Person.Auth(
            loginInfo = loginInfo,
            role = Role.User,
            activated = false
          )),
          meta = person.meta.update(person.id)
        )
        update(p).map(_ => p)
      }.getOrElse {
        val p = Person.from(form, loginInfo, avatar)
        create(p).map(_ => p)
      }
    }
  }

  def unregister(id: Person.Id): Future[Option[Person]] = {
    get(id).flatMap { personOpt =>
      personOpt.map { person =>
        val p = person.copy(auth = None, meta = person.meta.update(person.id))
        update(p).map(_ => Some(p))
      }.getOrElse {
        Future.successful(None)
      }
    }
  }

  def activate(id: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("auth.activated" -> true)), id)

  def setRole(id: Person.Id, role: Option[Person.Role.Value], by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("auth.role" -> role)), by)
}
object PersonRepository {
  val defaultSort: JsObject = Json.obj("data.name" -> 1)
  object Filters {
    private val fields = List("name", "twitter", "email", "description")
    def search(q: String): JsObject = Json.obj("$or" -> fields.map { f =>
      Json.obj(s"data.$f" -> Json.obj("$regex" -> (".*" + q + ".*"), "$options" -> "i"))
    })
  }
}
