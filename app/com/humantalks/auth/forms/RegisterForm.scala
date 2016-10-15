package com.humantalks.auth.forms

import play.api.data.Forms._

case class RegisterForm(
  name: String,
  email: String,
  password: String
)
object RegisterForm {
  val fields = mapping(
    "name" -> nonEmptyText,
    "email" -> email,
    "password" -> nonEmptyText
  )(RegisterForm.apply)(RegisterForm.unapply)
}
