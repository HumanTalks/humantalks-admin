package com.humantalks.infrastructure

import com.humantalks.common.Conf
import com.humantalks.domain.values.Meta
import com.humantalks.domain.{ User, VenueData, Venue }
import global.Contexts
import global.infrastructure.Mongo
import global.models.Page
import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult
import scala.concurrent.Future

case class VenueRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import ctx._
  import Contexts.dbToEC
  private val collection = db.getCollection(conf.Repositories.venue)
  private val defaultSort = Json.obj("name" -> 1)

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Venue]] =
    collection.find(filter, sort)

  def findByIds(ids: Seq[Venue.Id], sort: JsObject = defaultSort): Future[List[Venue]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids)), sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Venue]] =
    collection.findPage(index, size, filter, sort)

  def get(id: Venue.Id): Future[Option[Venue]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: VenueData, by: User.Id): Future[(WriteResult, Venue.Id)] = {
    val toCreate = Venue(Venue.Id.generate(), elt, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Venue, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(meta = elt.meta.update(by)))

  def partialUpdate(id: Venue.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))

  def delete(id: Venue.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
