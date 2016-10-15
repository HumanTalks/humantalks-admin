package com.humantalks.internal.venues

import com.humantalks.auth.entities.User
import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class VenueRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Venue, Venue.Id, Venue.Data] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.venue)
  val defaultSort = Json.obj("data.name" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Venue]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Venue]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Venue.Id], sort: JsObject = defaultSort): Future[List[Venue]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def get(id: Venue.Id): Future[Option[Venue]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: Venue.Data, by: User.Id): Future[(WriteResult, Venue.Id)] = {
    val toCreate = Venue(Venue.Id.generate(), elt.trim, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Venue, data: Venue.Data, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  /*def partialUpdate(id: Venue.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))*/

  def delete(id: Venue.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
