package com.humantalks.auth.forms

import play.api.data.Forms._

case class ResetPasswordForm(
  password: String
)
object ResetPasswordForm {
  val fields = mapping(
    "password" -> nonEmptyText
  )(ResetPasswordForm.apply)(ResetPasswordForm.unapply)
}
