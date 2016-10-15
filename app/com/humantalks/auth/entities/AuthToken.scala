package com.humantalks.auth.entities

import com.humantalks.internal.persons.Person
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.{ DateTime, DateTimeZone }
import play.api.libs.json.Json

import scala.concurrent.duration._

case class AuthToken(
  id: AuthToken.Id,
  person: Person.Id,
  expiry: DateTime
)
object AuthToken {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "AuthToken.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  def from(person: Person.Id, expiry: FiniteDuration = 10.minutes): AuthToken =
    AuthToken(AuthToken.Id.generate(), person, DateTime.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))

  implicit val format = Json.format[AuthToken]
}
