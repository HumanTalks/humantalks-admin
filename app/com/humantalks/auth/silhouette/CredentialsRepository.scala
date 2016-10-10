package com.humantalks.auth.silhouette

import com.humantalks.common.Conf
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import global.Contexts
import global.infrastructure.Mongo
import play.api.libs.json.{ JsObject, Json }

import scala.concurrent.Future

case class CredentialsRepository(conf: Conf, ctx: Contexts, db: Mongo) extends DelegableAuthInfoDAO[PasswordInfo] {
  import Contexts.dbToEC
  import ctx._
  private val collection = db.getCollection(conf.Repositories.credentials)
  private val defaultSort = Json.obj("loginInfo.providerID" -> 1, "loginInfo.providerKey" -> 1)
  val name = collection.name

  def find(filter: JsObject = Json.obj(), sort: JsObject = defaultSort): Future[List[Credentials]] =
    collection.find(filter, sort)

  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] =
    collection.get(Json.obj("loginInfo" -> loginInfo)).map(_.map(_.passwordInfo))

  def add(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] =
    collection.create(Credentials(loginInfo, passwordInfo)).map(_ => passwordInfo)

  def update(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] =
    collection.fullUpdate(Json.obj("loginInfo" -> loginInfo), Credentials(loginInfo, passwordInfo)).map(_ => passwordInfo)

  def save(loginInfo: LoginInfo, passwordInfo: PasswordInfo): Future[PasswordInfo] =
    collection.upsert(Json.obj("loginInfo" -> loginInfo), Credentials(loginInfo, passwordInfo)).map(_ => passwordInfo)

  def remove(loginInfo: LoginInfo): Future[Unit] =
    collection.delete(Json.obj("loginInfo" -> loginInfo)).map(_ => ())
}
