package global.infrastructure

import global.Contexts
import global.models.{ Page, Status }
import play.api.libs.json._
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.commands.{ Command, MultiBulkWriteResult, UpdateWriteResult, WriteResult }
import reactivemongo.api.{ BSONSerializationPack, QueryOpts, ReadPreference }
import reactivemongo.bson.BSONDocument
import reactivemongo.play.json._
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.Future

case class Mongo(ctx: Contexts, reactiveMongoApi: ReactiveMongoApi) {
  import Contexts.dbToEC
  import ctx._

  def getCollection[T](name: Repository.Collection[T]): MongoRepository[T] = MongoRepository[T](ctx, reactiveMongoApi, name.value)

  def ping(): Future[BSONDocument] = {
    reactiveMongoApi.database.flatMap { db =>
      val runner = Command.run(BSONSerializationPack)
      runner.apply(db, runner.rawCommand(BSONDocument("ping" -> 1))).one[BSONDocument]
    }
  }
  def pingStatus(): Future[Status] = {
    ping().map(MongoHelper.pingStatus).recover {
      case e: Throwable => Status(500, e.toString)
    }
  }
}

case class MongoRepository[T](ctx: Contexts, reactiveMongoApi: ReactiveMongoApi, name: String) {
  import Contexts.dbToEC
  import ctx._

  def jsonCollection(): Future[JSONCollection] = reactiveMongoApi.database.map(_.collection[JSONCollection](name))

  def find(filter: JsObject = Json.obj(), sort: JsObject = Json.obj())(implicit r: Reads[T]): Future[List[T]] =
    jsonCollection().flatMap { _.find(filter).sort(sort).cursor[T](ReadPreference.primary).collect[List]() }
  def findPage(index: Page.Index, size: Page.Size, filter: JsObject = Json.obj(), sort: JsObject = Json.obj())(implicit r: Reads[T]): Future[Page[T]] =
    jsonCollection().flatMap { collection =>
      for {
        items <- collection.find(filter).options(QueryOpts(index.firstItem(size), index.lastItem(size))).sort(sort).cursor[T](ReadPreference.primary).collect[List]()
        count <- collection.count(Some(filter))
      } yield Page(items, index, size, Page.Count(count))
    }
  def get(filter: JsObject)(implicit r: Reads[T]): Future[Option[T]] =
    jsonCollection().flatMap { _.find(filter).one[T] }
  def create(elt: T)(implicit w: OWrites[T]): Future[WriteResult] =
    jsonCollection().flatMap { _.insert(elt) }
  def fullUpdate(filter: JsObject, elt: T)(implicit w: OWrites[T]): Future[UpdateWriteResult] =
    jsonCollection().flatMap { _.update(filter, elt, upsert = false, multi = false) }
  def update(filter: JsObject, patch: JsObject)(implicit w: OWrites[T]): Future[UpdateWriteResult] =
    jsonCollection().flatMap { _.update(filter, patch, upsert = false, multi = false) }
  def upsert(filter: JsObject, elt: T)(implicit w: OWrites[T]): Future[UpdateWriteResult] =
    jsonCollection().flatMap { _.update(filter, elt, upsert = true, multi = false) }
  def updateAll(filter: JsObject, modifier: JsObject): Future[UpdateWriteResult] =
    jsonCollection().flatMap { _.update(filter, modifier, upsert = false, multi = true) }
  def delete(filter: JsObject): Future[WriteResult] =
    jsonCollection().flatMap { _.remove(filter, firstMatchOnly = true) }
  def deleteAll(filter: JsObject): Future[WriteResult] =
    jsonCollection().flatMap { _.remove(filter, firstMatchOnly = false) }
  def count(filter: JsObject = Json.obj()): Future[Int] =
    jsonCollection().flatMap { _.count(Some(filter)) }
  def bulkInsert(elts: Seq[T])(implicit w: OWrites[T]): Future[MultiBulkWriteResult] =
    jsonCollection().flatMap { _.bulkInsert(elts.map(e => w.writes(e).as[JsObject]).toStream, ordered = true) }
  def drop(): Future[Boolean] =
    jsonCollection().flatMap { _.drop(failIfNotFound = false) }

  def runCommand(command: JsObject): Future[List[JsValue]] = {
    reactiveMongoApi.database.flatMap { db =>
      val runner = Command.run(BSONSerializationPack)
      runner.apply(db, runner.rawCommand(MongoHelper.toBson(command)))
        .cursor[BSONDocument]
        .collect[List]()
        .map(_.map(MongoHelper.toJson))
    }
  }
}

object MongoHelper {
  def toBson(json: JsValue): BSONDocument = BSONFormats.BSONDocumentFormat.reads(json).get
  def toJson(bson: BSONDocument): JsValue = Json.toJson(bson)
  def pingStatus(res: BSONDocument): Status = {
    if (res.getAs[Double]("ok").contains(1d)) {
      Status(200, "ok")
    } else {
      Status(500, "Ping answering with " + Json.stringify(Json.toJson(res)))
    }
  }
}
