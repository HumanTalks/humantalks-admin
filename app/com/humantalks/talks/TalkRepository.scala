package com.humantalks.talks

import com.humantalks.common.Conf
import com.humantalks.common.models.User
import com.humantalks.common.models.values.Meta
import global.Contexts
import global.infrastructure.Mongo
import global.models.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class TalkRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.talk)
  private val defaultSort = Json.obj("name" -> 1)

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(filter, sort)

  def findByIds(ids: Seq[Talk.Id], sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Talk]] =
    collection.findPage(index, size, filter, sort)

  def get(id: Talk.Id): Future[Option[Talk]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: Talk.Data, by: User.Id): Future[(WriteResult, Talk.Id)] = {
    val toCreate = Talk(Talk.Id.generate(), elt.trim, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Talk, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(data = elt.data.trim, meta = elt.meta.update(by)))

  def partialUpdate(id: Talk.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))

  def delete(id: Talk.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
