package com.humantalks.auth.models

import com.mohiva.play.silhouette.api.Identity
import global.models.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.libs.json.Json

case class User(
  id: User.Id,
  data: User.Data,
  created: DateTime,
  updated: DateTime
) extends Identity
object User {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "User.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
    email: String,
    emailConfirmed: Boolean,
    password: String,
    rights: List[String]
  )

  val fake = User.Id("57b2edc0-3d2f-4cb3-94d0-b60c028738a4")

  implicit val formatData = Json.format[User.Data]
  implicit val format = Json.format[User]
  val fields = mapping(
    "email" -> email,
    "emailConfirmed" -> ignored(false),
    "password" -> nonEmptyText(minLength = 6),
    "rights" -> list(nonEmptyText)
  )(User.Data.apply)(User.Data.unapply)
}
