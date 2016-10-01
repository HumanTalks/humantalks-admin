package com.humantalks.infrastructure

import com.humantalks.common.Conf
import com.humantalks.domain.values.Meta
import com.humantalks.domain.{ User, MeetupData, Meetup }
import global.Contexts
import global.infrastructure.Mongo
import global.models.Page
import org.joda.time.DateTime
import play.api.libs.json.{ Json, JsObject }
import reactivemongo.api.commands.WriteResult
import scala.concurrent.Future

case class MeetupRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import ctx._
  import Contexts.dbToEC
  private val collection = db.getCollection(conf.Repositories.meetup)
  private val defaultSort = Json.obj("data.date" -> -1)

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(filter, sort)

  def findByIds(ids: Seq[Meetup.Id], sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids)), sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Meetup]] =
    collection.findPage(index, size, filter, sort)

  def get(id: Meetup.Id): Future[Option[Meetup]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: MeetupData, by: User.Id): Future[(WriteResult, Meetup.Id)] = {
    val toCreate = Meetup(Meetup.Id.generate(), elt, published = false, Meta(new DateTime(), by, new DateTime(), by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Meetup, by: User.Id): Future[WriteResult] =
    collection.fullUpdate(Json.obj("id" -> elt.id), elt.copy(meta = elt.meta.update(by)))

  def partialUpdate(id: Meetup.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))

  def delete(id: Meetup.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
