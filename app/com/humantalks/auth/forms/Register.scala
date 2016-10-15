package com.humantalks.auth.forms

import play.api.data.Forms._

case class Register(
  firstName: String,
  lastName: String,
  email: String,
  password: String
)
object Register {
  val fields = mapping(
    "firstName" -> nonEmptyText,
    "lastName" -> nonEmptyText,
    "email" -> email,
    "password" -> nonEmptyText
  )(Register.apply)(Register.unapply)
}
