package com.humantalks.internal.venues

import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class VenueRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Venue, Venue.Id, Venue.Data, Person.Id] {
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

  def create(elt: Venue.Data, by: Person.Id): Future[(WriteResult, Venue.Id)] = {
    val toCreate = Venue(Venue.Id.generate(), None, elt.trim, Meta.from(by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Venue, data: Venue.Data, by: Person.Id): Future[WriteResult] =
    collection.update(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  private def partialUpdate(id: Venue.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def setMeetupRef(id: Venue.Id, meetupRef: Venue.MeetupRef, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("meetupRef" -> meetupRef)), by)

  def delete(id: Venue.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
