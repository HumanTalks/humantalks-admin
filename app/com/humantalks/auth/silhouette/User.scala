package com.humantalks.auth.silhouette

import com.humantalks.auth.silhouette.forms.Register
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import global.values.{ TypedId, TypedIdHelper }
import org.joda.time.DateTime
import play.api.libs.json.Json

case class User(
    id: User.Id,
    loginInfo: LoginInfo,
    firstName: Option[String],
    lastName: Option[String],
    fullName: Option[String],
    email: Option[String],
    avatarURL: Option[String],
    activated: Boolean,
    created: DateTime,
    updated: DateTime
) extends Identity {
  def name: Option[String] = fullName.orElse {
    firstName -> lastName match {
      case (Some(f), Some(l)) => Some(f + " " + l)
      case (Some(f), None) => Some(f)
      case (None, Some(l)) => Some(l)
      case _ => None
    }
  }
  def merge(profile: CommonSocialProfile): User =
    this.copy(
      firstName = profile.firstName,
      lastName = profile.lastName,
      fullName = profile.fullName,
      email = profile.email,
      avatarURL = profile.avatarURL
    )
}
object User {
  val fake = User.Id("57b2edc0-3d2f-4cb3-94d0-b60c028738a4")
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "User.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  def from(profile: CommonSocialProfile): User =
    User(
      id = Id.generate(),
      loginInfo = profile.loginInfo,
      firstName = profile.firstName,
      lastName = profile.lastName,
      fullName = profile.fullName,
      email = profile.email,
      avatarURL = profile.avatarURL,
      activated = true,
      created = new DateTime(),
      updated = new DateTime()
    )
  def from(register: Register, loginInfo: LoginInfo): User =
    User(
      id = Id.generate(),
      loginInfo = loginInfo,
      firstName = Some(register.firstName),
      lastName = Some(register.lastName),
      fullName = Some(register.firstName + " " + register.lastName),
      email = Some(register.email),
      avatarURL = None,
      activated = false,
      created = new DateTime(),
      updated = new DateTime()
    )

  implicit val format = Json.format[User]
}
