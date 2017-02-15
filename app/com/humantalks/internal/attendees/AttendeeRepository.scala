package com.humantalks.internal.attendees

import com.humantalks.common.Conf
import com.humantalks.internal.events.{ Event, EventRepository }
import global.Contexts
import global.infrastructure.Mongo
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.{ MultiBulkWriteResult, WriteResult }

import scala.concurrent.Future

case class AttendeeRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.attendee)
  val name: String = collection.name

  def findByEvent(event: Event.Id, sort: JsObject = EventRepository.defaultSort): Future[List[Attendee]] =
    collection.find(Json.obj("event" -> event), sort)

  def importForEvent(event: Event.Id, attendees: List[Attendee]): Future[MultiBulkWriteResult] =
    collection.deleteMulti(Json.obj("event" -> event)).flatMap { _ =>
      collection.bulkInsert(attendees)
    }

  def checkin(event: Event.Id, id: Long): Future[WriteResult] =
    collection.partialUpdate(Json.obj("event" -> event, "id" -> id), Json.obj("$set" -> Json.obj("checkin" -> true)))
}
object AttendeeRepository {
  val defaultSort: JsObject = Json.obj("name" -> 1)
}
