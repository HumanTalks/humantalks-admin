package com.humantalks.auth.forms

import play.api.data.Forms._

case class RegisterForm(
  firstName: String,
  lastName: String,
  email: String,
  password: String
)
object RegisterForm {
  val fields = mapping(
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "email" -> email,
    "password" -> nonEmptyText
  )(RegisterForm.apply)(RegisterForm.unapply)
}
