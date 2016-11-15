package com.humantalks.internal.admin.config

import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import global.Contexts
import global.infrastructure.{ Repository, Mongo }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class ConfigRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Config, Config.Id, Config.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.config)
  val defaultSort = Json.obj("data.ref" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Config]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Config]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Config.Id], sort: JsObject = defaultSort): Future[List[Config]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def get(id: Config.Id): Future[Option[Config]] =
    collection.get(Json.obj("id" -> id))

  def getByRef(ref: String): Future[Option[Config]] =
    collection.get(Json.obj("data.ref" -> ref))

  def create(elt: Config.Data, by: Person.Id): Future[(WriteResult, Config.Id)] = {
    val toCreate = Config(Config.Id.generate(), elt.trim, Meta.from(by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Config, data: Config.Data, by: Person.Id): Future[WriteResult] =
    collection.update(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  private def partialUpdate(id: Config.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def setValue(id: Config.Id, value: String, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("data.value" -> value)), by)

  def delete(id: Config.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
