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
    meetupRef: Option[Person.MeetupRef],
    data: Person.Data,
    auth: Option[Person.Auth],
    meta: Meta
) extends Identity {
  def isUser: Boolean = auth.isDefined
  def isActivated: Boolean = auth.exists(_.activated)
  def hasProvider(provider: String): Boolean = auth.exists(_.loginInfo.providerID == provider)
  def hasRole(role: Person.Role.Value): Boolean = auth.exists(_.role == role)
  def isAuthorized(role: Person.Role.Value): Boolean = auth.exists(_.role >= role)
}
object Person {
  val anonymous = Id("ffffffff-ffff-ffff-ffff-ffffffffffff")
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Person.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class MeetupRef(group: String, id: Long)

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

  case class Auth(
    loginInfo: LoginInfo,
    role: Person.Role.Value,
    activated: Boolean
  )

  object Role extends Enumeration {
    val User, Organizer, Admin = Value
    val default = User
  }
  implicit val formatRole = EnumerationHelper.enumFormat(Role)

  object Shirt extends Enumeration {
    val XS_M, S_F, S_M, M_F, M_M, L_F, L_M, XL_M = Value
  }
  implicit val formatShirt = EnumerationHelper.enumFormat(Shirt)
  implicit val mappingShirt = EnumerationHelper.formMapping(Shirt)

  def from(register: RegisterForm, loginInfo: LoginInfo, avatar: Option[String] = None): Person = {
    val id = Id.generate()
    Person(
      id = id,
      meetupRef = None,
      data = Data(
        name = register.name,
        twitter = None,
        email = Some(register.email),
        phone = None,
        avatar = avatar,
        shirt = None,
        description = None
      ),
      auth = Some(Auth(
        loginInfo = loginInfo,
        role = Role.default,
        activated = false
      )),
      meta = Meta.from(id)
    )
  }
  def from(data: Person.Data, by: Person.Id): Person =
    Person(
      id = Person.Id.generate(),
      meetupRef = None,
      data = data.trim,
      auth = None,
      meta = Meta.from(by)
    )

  implicit val formatAuth = Json.format[Auth]
  implicit val formatData = Json.format[Data]
  implicit val formatMeetupRef = Json.format[MeetupRef]
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