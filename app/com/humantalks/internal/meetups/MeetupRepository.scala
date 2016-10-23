package com.humantalks.internal.meetups

import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class MeetupRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Meetup, Meetup.Id, Meetup.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.meetup)
  val defaultSort = Json.obj("data.date" -> -1)
  val name = collection.name

  /*def allIds(): Future[List[Meetup.Id]] =
    collection.jsonCollection().flatMap(_.find(Json.obj(), Json.obj("id" -> 1)).cursor[Meetup.Id](ReadPreference.primary).collect[List]())*/

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Meetup]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Meetup.Id], sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForTalk(id: Talk.Id, sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(Json.obj("data.talks" -> id), sort)

  def findForTalks(ids: Seq[Talk.Id], sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(Json.obj("data.talks" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForVenue(id: Venue.Id, sort: JsObject = defaultSort): Future[List[Meetup]] =
    collection.find(Json.obj("data.venue" -> id), sort)

  def get(id: Meetup.Id): Future[Option[Meetup]] =
    collection.get(Json.obj("id" -> id))

  def create(data: Meetup.Data, by: Person.Id): Future[(WriteResult, Meetup.Id)] = {
    val toCreate = Meetup(Meetup.Id.generate(), data.trim, published = false, Meta.from(by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Meetup, data: Meetup.Data, by: Person.Id): Future[WriteResult] =
    collection.update(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  private def partialUpdate(id: Meetup.Id, patch: JsObject, by: Person.Id, op: String = "$set"): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), Json.obj(op -> (patch - "id" - "meta")).deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def addTalk(id: Meetup.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("data.talks" -> talkId), by, "$addToSet")

  def moveTalk(id: Meetup.Id, talkId: Talk.Id, up: Boolean, by: Person.Id): Future[Option[WriteResult]] = {
    def swap[T](list: List[T], elt: T, before: Boolean): List[T] = list match {
      case Nil => Nil
      case prev :: `elt` :: tail if before => elt :: prev :: tail
      case `elt` :: next :: tail if !before => next :: elt :: tail
      case head :: tail => head :: swap(tail, elt, before)
    }
    get(id).flatMap {
      _.map { meetup =>
        update(meetup, meetup.data.copy(talks = swap(meetup.data.talks, talkId, up)), by).map(Some(_))
      }.getOrElse {
        Future.successful(None)
      }
    }
  }

  def removeTalk(id: Meetup.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("data.talks" -> talkId), by, "$pull")

  def setPublished(id: Meetup.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("published" -> true), by)

  def delete(id: Meetup.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
