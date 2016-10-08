package com.humantalks.auth.silhouette.forms

import play.api.data.Forms._

case class Login(
  email: String,
  password: String,
  rememberMe: Boolean
)
object Login {
  val fields = mapping(
    "email" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  )(Login.apply)(Login.unapply)
}