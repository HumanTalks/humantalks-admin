package com.humantalks.auth.silhouette

import global.models.{ TypedIdHelper, TypedId }
import org.joda.time.DateTime
import play.api.libs.json.Json

case class AuthToken(
  id: AuthToken.Id,
  userId: User.Id,
  expiry: DateTime
)
object AuthToken {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "AuthToken.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  implicit val format = Json.format[AuthToken]
}
