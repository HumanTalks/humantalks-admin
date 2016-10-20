package com.humantalks.internal.persons

import com.humantalks.auth.forms.RegisterForm
import com.humantalks.common.values.Meta
import com.humantalks.common.services.TwitterSrv
import com.mohiva.play.silhouette.api.{ Identity, LoginInfo }
import global.helpers.EnumerationHelper
import global.values.{ TypedId, TypedIdHelper }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Person(
    id: Person.Id,
    data: Person.Data,
    loginInfo: Option[LoginInfo],
    role: Option[Person.Role.Value],
    activated: Boolean,
    meta: Meta
) extends Identity {
  def hasProvider(provider: String): Boolean = loginInfo.exists(_.providerID == provider)
}
object Person {
  val anonymous = Id("ffffffff-ffff-ffff-ffff-ffffffffffff")
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Person.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  object Role extends Enumeration {
    val User, Organizer, Admin = Value
  }

  case class Data(
      name: String,
      twitter: Option[String],
      email: Option[String], // to match existing person when submiting a new talk
      phone: Option[String],
      avatar: Option[String],
      description: Option[String]
  ) {
    def trim: Data = this.copy(
      name = this.name.trim,
      twitter = this.twitter.map(TwitterSrv.toAccount),
      email = this.email.map(_.trim),
      phone = this.phone.map(_.trim),
      avatar = this.avatar.map(_.trim),
      description = this.description.map(_.trim)
    )
  }

  def from(register: RegisterForm, loginInfo: LoginInfo, avatar: Option[String] = None): Person = {
    val id = Id.generate()
    Person(
      id = id,
      data = Data(
        name = register.name,
        twitter = None,
        email = Some(register.email),
        phone = None,
        avatar = avatar,
        description = None
      ),
      loginInfo = Some(loginInfo),
      activated = false,
      role = Some(Role.User),
      meta = Meta.from(id)
    )
  }
  def from(data: Person.Data, by: Person.Id): Person =
    Person(
      id = Person.Id.generate(),
      data = data.trim,
      loginInfo = None,
      activated = false,
      role = Some(Role.User),
      meta = Meta.from(by)
    )

  implicit val formatRole = EnumerationHelper.enumFormat(Role)
  implicit val formatData = Json.format[Person.Data]
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "email" -> optional(email),
    "phone" -> optional(text),
    "avatar" -> optional(text),
    "description" -> optional(text)
  )(Person.Data.apply)(Person.Data.unapply)
}