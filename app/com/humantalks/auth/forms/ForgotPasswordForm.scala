package com.humantalks.auth.forms

import play.api.data.Forms._

case class ForgotPasswordForm(
  email: String
)
object ForgotPasswordForm {
  val fields = mapping(
    "email" -> email
  )(ForgotPasswordForm.apply)(ForgotPasswordForm.unapply)
}
