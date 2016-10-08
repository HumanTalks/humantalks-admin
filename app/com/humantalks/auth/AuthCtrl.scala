package com.humantalks.auth

import com.humantalks.auth.helpers.{ AuthEnv, AuthController }
import com.humantalks.auth.models.User
import com.mohiva.play.silhouette.api.Silhouette
import play.api.data.Form
import play.api.i18n.MessagesApi

case class AuthCtrl(silhouette: Silhouette[AuthEnv])(implicit messages: MessagesApi) extends AuthController {
  def messagesApi: MessagesApi = messages
  val userForm = Form(User.fields)

  def register = UserAwareAction { implicit request =>
    request.identity match {
      case Some(_) => Redirect(com.humantalks.common.controllers.routes.Application.index)
      case None => Ok(views.html.register(userForm))
    }
  }
}
