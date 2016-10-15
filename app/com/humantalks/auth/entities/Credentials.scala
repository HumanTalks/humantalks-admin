package com.humantalks.auth.entities

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import play.api.libs.json.Json

case class Credentials(
  loginInfo: LoginInfo,
  passwordInfo: PasswordInfo
)
object Credentials {
  implicit val formatLoginInfo = Json.format[LoginInfo]
  implicit val formatPasswordInfo = Json.format[PasswordInfo]
  implicit val format = Json.format[Credentials]
}
