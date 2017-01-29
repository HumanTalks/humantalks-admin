package com.humantalks.internal.events

import com.humantalks.common.Conf
import com.humantalks.common.values.{ GMapPlace, Meta }
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.partners.Partner
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class EventRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Event, Event.Id, Event.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.event)
  val defaultSort = Json.obj("data.date" -> -1)
  val name = collection.name

  /*def allIds(): Future[List[Event.Id]] =
    collection.jsonCollection().flatMap(_.find(Json.obj(), Json.obj("id" -> 1)).cursor[Event.Id](ReadPreference.primary).collect[List]())*/

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Event]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Event.Id], sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForTalk(id: Talk.Id, sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(Json.obj("data.talks" -> id), sort)

  def findForTalks(ids: Seq[Talk.Id], sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(Json.obj("data.talks" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForPartner(id: Partner.Id, sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(Json.obj("data.venue" -> id), sort)

  def findPast(sort: JsObject = defaultSort): Future[List[Event]] =
    collection.find(Json.obj("data.date" -> Json.obj("$lt" -> new DateTime())), sort)

  def get(id: Event.Id): Future[Option[Event]] =
    collection.get(Json.obj("id" -> id))

  def getLast: Future[Option[Event]] =
    collection.getOne(sort = Json.obj("data.date" -> -1))

  def getNext: Future[Option[Event]] =
    collection.getOne(filter = Json.obj("data.date" -> Json.obj("$gt" -> new DateTime())), sort = Json.obj("data.date" -> 1))

  def create(data: Event.Data, by: Person.Id): Future[(WriteResult, Event.Id)] = {
    val toCreate = Event(Event.Id.generate(), meetupRef = None, data.trim, Meta.from(by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Event, data: Event.Data, by: Person.Id): Future[WriteResult] =
    collection.update(Json.obj("id" -> elt.id), elt.copy(data = data.trim, meta = elt.meta.update(by)))

  private def partialUpdate(id: Event.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def setVenue(id: Event.Id, partnerId: Partner.Id, location: Option[GMapPlace], by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("data.venue" -> partnerId, "data.location" -> location)), by)

  def addTalk(id: Event.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$addToSet" -> Json.obj("data.talks" -> talkId)), by)

  def moveTalk(id: Event.Id, talkId: Talk.Id, up: Boolean, by: Person.Id): Future[Option[WriteResult]] = {
    def swap[T](list: List[T], elt: T, before: Boolean): List[T] = list match {
      case Nil => Nil
      case prev :: `elt` :: tail if before => elt :: prev :: tail
      case `elt` :: next :: tail if !before => next :: elt :: tail
      case head :: tail => head :: swap(tail, elt, before)
    }
    get(id).flatMap {
      _.map { event =>
        update(event, event.data.copy(talks = swap(event.data.talks, talkId, up)), by).map(Some(_))
      }.getOrElse {
        Future.successful(None)
      }
    }
  }

  def removeTalk(id: Event.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$pull" -> Json.obj("data.talks" -> talkId)), by)

  def setMeetupRef(id: Event.Id, meetupRef: Event.MeetupRef, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("meetupRef" -> meetupRef)), by)

  def unsetMeetupRef(id: Event.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$unset" -> Json.obj("meetupRef" -> "")), by)

  def delete(id: Event.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
