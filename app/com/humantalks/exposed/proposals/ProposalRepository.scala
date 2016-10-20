package com.humantalks.exposed.proposals

import com.humantalks.common.Conf
import com.humantalks.common.services.EmbedSrv
import com.humantalks.internal.persons.Person
import global.Contexts
import global.infrastructure.Mongo
import global.values.{ ApiError, Page }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class ProposalRepository(conf: Conf, ctx: Contexts, db: Mongo, embedSrv: EmbedSrv) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.proposal)
  val defaultSort = Json.obj("created" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Proposal]] =
    collection.findPage(index, size, filter, sort)

  def findByIds(ids: Seq[Proposal.Id], sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForPerson(id: Person.Id, sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(Json.obj("data.speakers" -> id), sort)

  def get(id: Proposal.Id): Future[Option[Proposal]] =
    collection.get(Json.obj("id" -> id))

  def create(elt: Proposal.Data): Future[(WriteResult, Proposal)] =
    fillEmbedCode(Proposal(Proposal.Id.generate(), elt.trim, new DateTime(), new DateTime())).flatMap { toCreate =>
      collection.create(toCreate).map { res => (res, toCreate) }
    }

  def update(elt: Proposal, data: Proposal.Data): Future[WriteResult] =
    fillEmbedCode(elt.copy(data = data.trim, updated = new DateTime())).flatMap { toUpdate =>
      collection.update(Json.obj("id" -> elt.id), toUpdate)
    }

  /*def partialUpdate(id: Proposal.Id, patch: JsObject): Future[WriteResult] =
    collection.update(Json.obj("id" -> id), Json.obj("$set" -> (patch - "id")))*/

  def delete(id: Proposal.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))

  private def fillEmbedCode(proposal: Proposal): Future[Proposal] = {
    val slidesEmbedFut = proposal.data.slides.map(url => embedSrv.embedRemote(url)).getOrElse(Future(Left(ApiError.emtpy)))
    val videoEmbedFut = proposal.data.video.map(url => embedSrv.embedRemote(url)).getOrElse(Future(Left(ApiError.emtpy)))
    for {
      slidesEmbed <- slidesEmbedFut
      videoEmbed <- videoEmbedFut
    } yield proposal.copy(data = proposal.data.copy(
      slidesEmbedCode = slidesEmbed.right.toOption.map(_.embedCode),
      videoEmbedCode = videoEmbed.right.toOption.map(_.embedCode)
    ))
  }
}
