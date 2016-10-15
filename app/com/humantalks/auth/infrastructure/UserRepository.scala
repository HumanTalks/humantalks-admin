package com.humantalks.auth.infrastructure

import com.humantalks.auth.entities.User
import com.humantalks.common.Conf
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import global.Contexts
import global.infrastructure.Mongo
import org.joda.time.DateTime
import play.api.libs.json.{ JsObject, Json }
import reactivemongo.api.commands.WriteResult

import scala.concurrent.Future

case class UserRepository(conf: Conf, ctx: Contexts, db: Mongo) extends IdentityService[User] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.user)
  private val defaultSort = Json.obj("lastName" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[User]] =
    collection.find(filter, sort)

  def findByIds(ids: Seq[User.Id], sort: JsObject = defaultSort): Future[List[User]] =
    collection.find(Json.obj("id" -> Json.obj("$in" -> ids.distinct)), sort)

  def get(id: User.Id): Future[Option[User]] =
    collection.get(Json.obj("id" -> id))

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] =
    collection.get(Json.obj("loginInfo.providerID" -> loginInfo.providerID, "loginInfo.providerKey" -> loginInfo.providerKey))

  def create(user: User): Future[User] =
    collection.create(user).map(_ => user)

  def update(user: User): Future[User] =
    collection.update(Json.obj("id" -> user.id), user.copy(updated = new DateTime())).map(_ => user)

  def activate(id: User.Id): Future[WriteResult] =
    collection.partialUpdate(Json.obj("id" -> id), Json.obj("$set" -> Json.obj("activated" -> true)))

  def delete(id: User.Id): Future[WriteResult] =
    collection.delete(Json.obj("id" -> id))
}
