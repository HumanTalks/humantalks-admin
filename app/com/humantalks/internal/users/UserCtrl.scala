package com.humantalks.internal.users

import com.humantalks.auth.infrastructure.UserRepository
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import play.api.i18n.MessagesApi
import play.api.mvc.Controller

import scala.concurrent.Future

case class UserCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    userRepository: UserRepository
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def profil = silhouette.SecuredAction.async { implicit req =>
    Future(Ok(views.html.profil(req.identity)))
  }
}
