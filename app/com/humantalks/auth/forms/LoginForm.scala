package com.humantalks.auth.forms

import play.api.data.Forms._

case class LoginForm(
  email: String,
  password: String,
  rememberMe: Boolean
)
object LoginForm {
  val fields = mapping(
    "email" -> email,
    "password" -> nonEmptyText,
    "rememberMe" -> boolean
  )(LoginForm.apply)(LoginForm.unapply)
}