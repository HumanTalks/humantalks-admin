package com.humantalks.internal.partners

import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import global.Contexts
import global.helpers.JsonHelper
import global.infrastructure.{ Mongo, Repository }
import global.values.Page
import org.joda.time.{ DateTime, LocalDate }
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class PartnerRepository(conf: Conf, ctx: Contexts, db: Mongo) extends Repository[Partner, Partner.Id, Partner.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.partner)
  val name: String = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = PartnerRepository.defaultSort): Future[List[Partner]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = PartnerRepository.defaultSort): Future[Page[Partner]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Partner.Id], sort: JsObject = PartnerRepository.defaultSort): Future[List[Partner]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findSponsors(sort: JsObject = PartnerRepository.defaultSort): Future[List[Partner]] =
    collection.find(PartnerRepository.Filters.isSponsor(), sort)

  def findSponsorsAtDate(date: LocalDate, sort: JsObject = PartnerRepository.defaultSort): Future[List[Partner]] =
    collection.find(PartnerRepository.Filters.isSponsor(date), sort)

  def get(id: Partner.Id): Future[Option[Partner]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: Partner.Data, by: Person.Id): Future[(WriteResult, Partner.Id)] = {
    val toCreate = Partner(Partner.Id.generate(), None, elt.trim, Meta.from(by))
    collection.create(toCreate).map { res => (res, toCreate.id) }
  }

  def update(elt: Partner, data: Partner.Data, by: Person.Id): Future[WriteResult] = {
    val json = Json.toJson(data).as[JsObject] - "venue" - "sponsoring"
    partialUpdate(elt.id, Json.obj("$set" -> JsonHelper.prefixKeys(json, 'data)), by)
  }

  def updateVenue(id: Partner.Id, venue: Partner.Venue, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("data.venue" -> venue)), by)

  def addSponsor(id: Partner.Id, sponsor: Partner.Sponsor, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$addToSet" -> Json.obj("data.sponsoring" -> sponsor)), by)

  def updateSponsor(id: Partner.Id, index: Int, sponsor: Partner.Sponsor, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj(s"data.sponsoring.$index" -> sponsor)), by)

  def removeSponsor(id: Partner.Id, index: Int, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$unset" -> Json.obj(s"data.sponsoring.$index" -> 1)), by).flatMap { _ =>
      partialUpdate(id, Json.obj("$pull" -> Json.obj(s"data.sponsoring" -> JsNull)), by)
    }

  private def partialUpdate(id: Partner.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def setMeetupRef(id: Partner.Id, meetupRef: Partner.MeetupRef, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("meetupRef" -> meetupRef)), by)

  def delete(id: Partner.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
object PartnerRepository {
  val defaultSort: JsObject = Json.obj("data.name" -> 1)
  object Filters {
    def isSponsor(): JsObject = Json.obj(
      "data.sponsoring.1" -> Json.obj("$exists" -> true)
    )
    def isSponsor(date: LocalDate): JsObject = Json.obj(
      "data.sponsoring.start" -> Json.obj("$lt" -> date),
      "data.sponsoring.end" -> Json.obj("$gt" -> date)
    )
    def isVenue: JsObject = Json.obj(
      "data.venue" -> Json.obj("$exists" -> true)
    )
    private val fields = List("name", "twitter", "comment", "contacts", "venue.location.formatted", "venue.contact", "venue.comment")
    def search(q: String): JsObject = Json.obj("$or" -> fields.map { f =>
      Json.obj(s"data.$f" -> Json.obj("$regex" -> (".*" + q + ".*"), "$options" -> "i"))
    })
  }
}
