package com.humantalks.internal.talks

import com.humantalks.common.Conf
import com.humantalks.common.values.Meta
import com.humantalks.common.services.EmbedSrv
import com.humantalks.exposed.proposals.Proposal
import com.humantalks.internal.persons.Person
import global.Contexts
import global.infrastructure.{ Mongo, Repository }
import global.values.{ ApiError, Page }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class TalkRepository(conf: Conf, ctx: Contexts, db: Mongo, embedSrv: EmbedSrv) extends Repository[Talk, Talk.Id, Talk.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.talk)
  val defaultSort = Json.obj("data.title" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Talk]] =
    collection.findPage(index, size, filter, sort)

  def findPending(sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(Json.obj("status" -> Json.obj("$in" -> Json.arr(Talk.Status.Proposed, Talk.Status.Accepted))), sort)

  def findByIds(ids: Seq[Talk.Id], sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForPerson(id: Person.Id, sort: JsObject = defaultSort): Future[List[Talk]] =
    collection.find(Json.obj("data.speakers" -> id), sort)

  def get(id: Talk.Id): Future[Option[Talk]] =
    collection.get(Json.obj("id" -> id))

  def getLastAccepted: Future[Option[Talk]] =
    collection.getOne(Json.obj("status" -> Talk.Status.Accepted), Json.obj("meta.updated" -> -1))

  def create(elt: Talk.Data, by: Person.Id): Future[(WriteResult, Talk.Id)] =
    fillEmbedCode(Talk(Talk.Id.generate(), Talk.Status.Proposed, elt.trim, Meta.from(by))).flatMap { toCreate =>
      collection.create(toCreate).map { res => (res, toCreate.id) }
    }

  def create(elt: Proposal.Data, by: Person.Id): Future[(WriteResult, Talk.Id)] =
    fillEmbedCode(Talk(Talk.Id.generate(), Talk.Status.Proposed, elt.toTalk.trim, Meta.from(by))).flatMap { toCreate =>
      collection.create(toCreate).map { res => (res, toCreate.id) }
    }

  def update(elt: Talk, data: Talk.Data, by: Person.Id): Future[WriteResult] =
    fillEmbedCode(elt.copy(data = data.trim, meta = elt.meta.update(by))).flatMap { toUpdate =>
      collection.update(Json.obj("id" -> elt.id), toUpdate)
    }

  private def partialUpdate(id: Talk.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def updateAttribute(id: Talk.Id, attribute: String, value: String, by: Person.Id): Future[WriteResult] = {
    attribute match {
      case "video" | "slides" => embedSrv.embedRemote(value).flatMap { embed =>
        partialUpdate(id, Json.obj("$set" -> Json.obj(
          s"data.$attribute" -> value,
          s"data.${attribute}EmbedCode" -> embed.right.toOption.map(_.embedCode)
        )), by)
      }
      case "title" | "description" => partialUpdate(id, Json.obj("$set" -> Json.obj(s"data.$attribute" -> value)), by)
      case _ => Future.failed(new IllegalArgumentException(s"Unable to update attribute '$attribute'"))
    }
  }

  def setStatus(id: Talk.Id, status: Talk.Status.Value, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("status" -> status)), by)

  def delete(id: Talk.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))

  private def fillEmbedCode(talk: Talk): Future[Talk] = {
    val slidesEmbedFut = talk.data.slides.map(url => embedSrv.embedRemote(url)).getOrElse(Future.successful(Left(ApiError.emtpy)))
    val videoEmbedFut = talk.data.video.map(url => embedSrv.embedRemote(url)).getOrElse(Future.successful(Left(ApiError.emtpy)))
    for {
      slidesEmbed <- slidesEmbedFut
      videoEmbed <- videoEmbedFut
    } yield talk.copy(data = talk.data.copy(
      slidesEmbedCode = slidesEmbed.right.toOption.map(_.embedCode),
      videoEmbedCode = videoEmbed.right.toOption.map(_.embedCode)
    ))
  }
}
