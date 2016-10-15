package com.humantalks.internal.admin

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.entities.AuthToken
import com.humantalks.auth.infrastructure.{ AuthTokenRepository, CredentialsRepository }
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import play.api.i18n.MessagesApi
import play.api.mvc.Controller

case class AdminCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    credentialsRepository: CredentialsRepository,
    authTokenRepository: AuthTokenRepository
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def users = silhouette.SecuredAction(WithRole(Person.Role.Admin)).async { implicit req =>
    implicit val user = Some(req.identity)
    for {
      users <- personDbService.findUsers()
      credentials <- credentialsRepository.find()
      authTokens <- authTokenRepository.find()
    } yield Ok(views.html.userList(users, credentials, authTokens))
  }

  def setRole(id: Person.Id) = silhouette.SecuredAction(WithRole(Person.Role.Admin)).async { implicit req =>
    implicit val user = Some(req.identity)
    val role = req.body.asFormUrlEncoded.get("role").headOption.filter(_.length > 0).map(Person.Role.withName)
    personDbService.setRole(id, role).map { _ =>
      Redirect(routes.AdminCtrl.users())
    }
  }

  def deleteToken(id: AuthToken.Id) = silhouette.SecuredAction(WithRole(Person.Role.Admin)).async { implicit req =>
    implicit val user = Some(req.identity)
    authTokenRepository.delete(id).map { _ =>
      Redirect(routes.AdminCtrl.users())
    }
  }
}
