package com.humantalks.auth.silhouette

import global.models.{ TypedIdHelper, TypedId }
import org.joda.time.{ DateTimeZone, DateTime }
import play.api.libs.json.Json

import scala.concurrent.duration._

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

  def from(userId: User.Id, expiry: FiniteDuration = 5.minutes): AuthToken =
    AuthToken(AuthToken.Id.generate(), userId, DateTime.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))

  implicit val format = Json.format[AuthToken]
}
