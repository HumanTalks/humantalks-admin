package com.humantalks.persons

import com.humantalks.auth.models.User
import com.humantalks.common.Conf
import com.humantalks.common.models.Meta
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.models.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class PersonRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Person, Person.Id, Person.Data] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.person)
  private val defaultSort = Json.obj("data.name" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Person]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Person]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Person.Id], sort: JsObject = defaultSort): Future[List[Person]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def get(id: Person.Id): Future[Option[Person]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: Person.Data, by: User.Id): Future[(WriteResult, Person.Id)] = {
    val toCreate = Person(Person.Id.generate(), elt.trim, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Person, data: Person.Data, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  /*def partialUpdate(id: Person.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))*/

  def delete(id: Person.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
