package com.humantalks.auth.silhouette

import com.humantalks.common.Conf
import global.Contexts
import global.infrastructure.Mongo
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.Future

case class AuthTokenRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.authToken)

  def get(id: AuthToken.Id): Future[Option[AuthToken]] =
    collection.get(Json.obj("id" -> id))

  def findExpired(dateTime: DateTime): Future[Seq[AuthToken]] =
    collection.find(Json.obj("expiry" -> Json.obj("$lt" -> dateTime)))

  def create(token: AuthToken): Future[AuthToken] =
    collection.create(token).map(_ => token)

  def delete(id: AuthToken.Id): Future[Unit] =
    collection.delete(Json.obj("id" -> id)).map(_ => ())

  def deleteExpired(dateTime: DateTime): Future[Unit] =
    collection.delete(Json.obj("expiry" -> Json.obj("$lt" -> dateTime))).map(_ => ())
}
