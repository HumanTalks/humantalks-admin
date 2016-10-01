package com.humantalks.infrastructure

import com.humantalks.common.Conf
import com.humantalks.domain.values.Meta
import com.humantalks.domain.{ User, PersonData, Person }
import global.Contexts
import global.infrastructure.Mongo
import global.models.Page
import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class PersonRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import ctx._
  import Contexts.dbToEC
  private val collection = db.getCollection(conf.Repositories.person)
  private val defaultSort = Json.obj("name" -> 1)

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Person]] =
    collection.find(filter, sort)

  def findByIds(ids: Seq[Person.Id], sort: JsObject = defaultSort): Future[List[Person]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids)), sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Person]] =
    collection.findPage(index, size, filter, sort)

  def get(id: Person.Id): Future[Option[Person]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: PersonData, by: User.Id): Future[(WriteResult, Person.Id)] = {
    val toCreate = Person(Person.Id.generate(), elt, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Person, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(meta = elt.meta.update(by)))

  def partialUpdate(id: Person.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))

  def delete(id: Person.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
