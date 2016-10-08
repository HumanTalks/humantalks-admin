package com.humantalks.auth.silhouette

import java.util.UUID

import com.humantalks.common.Conf
import com.mohiva.play.silhouette.api.LoginInfo
import global.Contexts
import global.infrastructure.Mongo
import org.joda.time.DateTime
import play.api.libs.json.Json

import scala.concurrent.Future

case class UserRepository(conf: Conf, ctx: Contexts, db: Mongo) {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.user)

  def get(id: User.Id): Future[Option[User]] =
    collection.get(Json.obj("id" -> id))

  def get(loginInfo: LoginInfo): Future[Option[User]] =
    collection.get(Json.obj("loginInfo.providerID" -> loginInfo.providerID, "loginInfo.providerKey" -> loginInfo.providerKey))

  def create(user: User): Future[User] =
    collection.create(user).map(_ => user)

  def update(user: User): Future[User] =
    collection.fullUpdate(Json.obj("id" -> user.id), user.copy(updated = new DateTime())).map(_ => user)

  def upsert(user: User): Future[User] =
    collection.upsert(Json.obj("id" -> user.id), user.copy(updated = new DateTime())).map(_ => user)
}
