package com.humantalks.exposed.proposals

import com.humantalks.common.Conf
import com.humantalks.common.services.EmbedSrv
import com.humantalks.common.values.Meta
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import global.Contexts
import global.infrastructure.{ Repository, Mongo }
import global.values.{ ApiError, Page }
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class ProposalRepository(conf: Conf, ctx: Contexts, db: Mongo, embedSrv: EmbedSrv) extends Repository[Proposal, Proposal.Id, Proposal.Data, Person.Id] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.proposal)
  val defaultSort = Json.obj("meta.created" -> -1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(filter, sort)

  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[Page[Proposal]] =
    collection.findPage(index, size, filter, sort)

  def findPending(sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(Json.obj("status" -> Proposal.Status.Proposed), sort)

  def findByIds(ids: Seq[Proposal.Id], sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def findForPerson(id: Person.Id, sort: JsObject = defaultSort): Future[List[Proposal]] =
    collection.find(Json.obj("data.speakers" -> id), sort)

  def get(id: Proposal.Id): Future[Option[Proposal]] =
    collection.get(Json.obj("id" -> id))

  def getLast: Future[Option[Proposal]] =
    collection.getOne(sort = Json.obj("meta.created" -> -1))

  def getForTalk(id: Talk.Id): Future[Option[Proposal]] =
    collection.get(Json.obj("talk" -> id))

  def create(elt: Proposal.Data, by: Person.Id): Future[(WriteResult, Proposal.Id)] =
    fillEmbedCode(Proposal(Proposal.Id.generate(), Proposal.Status.Proposed, elt.trim, None, Meta.from(by))).flatMap { toCreate =>
      collection.create(toCreate).map { res => (res, toCreate.id) }
    }

  def update(elt: Proposal, data: Proposal.Data, by: Person.Id): Future[WriteResult] =
    fillEmbedCode(elt.copy(data = data.trim, meta = elt.meta.update(by))).flatMap { toUpdate =>
      collection.update(Json.obj("id" -> elt.id), toUpdate)
    }

  private def partialUpdate(id: Proposal.Id, patch: JsObject, by: Person.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), patch.deepMerge(Json.obj("$set" -> Json.obj("meta.updated" -> new DateTime(), "meta.updatedBy" -> by))))

  def setStatus(id: Proposal.Id, status: Proposal.Status.Value, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("status" -> status)), by)

  def setTalk(id: Proposal.Id, talkId: Talk.Id, by: Person.Id): Future[WriteResult] =
    partialUpdate(id, Json.obj("$set" -> Json.obj("talk" -> talkId, "status" -> Proposal.Status.Accepted)), by)

  def delete(id: Proposal.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))

  private def fillEmbedCode(proposal: Proposal): Future[Proposal] = {
    val slidesEmbedFut = proposal.data.slides.map(url => embedSrv.embedRemote(url)).getOrElse(Future.successful(Left(ApiError.emtpy)))
    for {
      slidesEmbed <- slidesEmbedFut
    } yield proposal.copy(data = proposal.data.copy(
      slidesEmbedCode = slidesEmbed.right.toOption.map(_.embedCode)
    ))
  }
}
