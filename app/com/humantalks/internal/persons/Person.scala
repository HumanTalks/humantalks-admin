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
  implicit val formatRole = EnumerationHelper.enumFormat(Role)

  object Shirt extends Enumeration {
    val XS_M, S_F, S_M, M_F, M_M, L_F, L_M, XL_M = Value
  }
  implicit val formatShirt = EnumerationHelper.enumFormat(Shirt)
  implicit val mappingShirt = EnumerationHelper.formMapping(Shirt)

  case class Data(
      name: String,
      twitter: Option[String],
      email: Option[String], // to match existing person when submiting a new talk
      phone: Option[String],
      avatar: Option[String],
      shirt: Option[Person.Shirt.Value],
      description: Option[String]
  ) {
    def trim: Data = copy(
      name = name.trim,
      twitter = twitter.map(TwitterSrv.toAccount),
      email = email.map(_.trim),
      phone = phone.map(_.trim),
      avatar = avatar.map(_.trim),
      description = description.map(_.trim)
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
        shirt = None,
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

  implicit val formatData = Json.format[Person.Data]
  implicit val format = Json.format[Person]
  val fields = mapping(
    "name" -> nonEmptyText,
    "twitter" -> optional(text),
    "email" -> optional(email),
    "phone" -> optional(text),
    "avatar" -> optional(text),
    "shirt" -> optional(of[Person.Shirt.Value]),
    "description" -> optional(text)
  )(Person.Data.apply)(Person.Data.unapply)
}