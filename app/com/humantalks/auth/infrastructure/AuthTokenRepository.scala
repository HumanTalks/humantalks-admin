package com.humantalks.auth.infrastructure

import com.humantalks.auth.entities.AuthToken
import com.humantalks.common.Conf
import com.humantalks.internal.persons.Person
import global.Contexts
import global.infrastructure.Mongo
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future

case class AuthTokenRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.authToken)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = AuthTokenRepository.defaultSort): Future[List[AuthToken]] =
    collection.find(filter, sort)

  def findExpired(): Future[Seq[AuthToken]] =
    collection.find(Json.obj("expiry" -> Json.obj("$lt" -> new DateTime)))

  def get(id: AuthToken.Id): Future[Option[AuthToken]] =
    collection.get(Json.obj("id" -> id, "expiry" -> Json.obj("$gt" -> new DateTime)))

  def create(token: AuthToken): Future[AuthToken] =
    collection.create(token).map(_ => token)

  def create(id: Person.Id): Future[AuthToken] =
    create(AuthToken.from(id))

  def delete(id: AuthToken.Id): Future[Unit] =
    collection.delete(Json.obj("id" -> id)).map(_ => ())

  def delete(id: Person.Id): Future[Unit] =
    collection.delete(Json.obj("person" -> id)).map(_ => ())

  def deleteExpired(): Future[Unit] =
    collection.delete(Json.obj("expiry" -> Json.obj("$lt" -> new DateTime))).map(_ => ())
}
object AuthTokenRepository {
  val defaultSort = Json.obj("expiry" -> 1)
}
