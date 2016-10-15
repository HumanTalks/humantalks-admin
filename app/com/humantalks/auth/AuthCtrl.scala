package com.humantalks.auth

import java.net.URLDecoder

import com.humantalks.auth.entities.{ AuthToken, User }
import com.humantalks.auth.infrastructure.{ UserRepository, CredentialsRepository, AuthTokenRepository }
import com.humantalks.auth.forms.{ LoginForm, RegisterForm }
import com.humantalks.auth.services.MailerSrv
import com.humantalks.auth.silhouette._
import com.mohiva.play.silhouette.api.actions.UserAwareRequest
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import global.Contexts
import org.joda.time.DateTime
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{ AnyContent, Action, Controller }

import scala.concurrent.Future

case class AuthCtrl(
    conf: SilhouetteConf,
    ctx: Contexts,
    userRepository: UserRepository,
    credentialsRepository: CredentialsRepository,
    authTokenRepository: AuthTokenRepository,
    silhouette: Silhouette[SilhouetteEnv],
    passwordHasherRegistry: PasswordHasherRegistry,
    avatarService: AvatarService,
    authInfoRepository: AuthInfoRepository,
    credentialsProvider: CredentialsProvider,
    mailerSrv: MailerSrv
)(implicit messagesApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val registerForm = Form(RegisterForm.fields)
  val loginForm = Form(LoginForm.fields)
  val emailFrom = "HumanTalks <paris@humantalks.com>"
  val loginRedirect = Redirect(com.humantalks.internal.routes.Application.index())
  val logoutRedirect = Redirect(com.humantalks.exposed.routes.Application.index())

  def register() = silhouette.UserAwareAction.async { implicit req =>
    req.identity match {
      case Some(user) => Future(loginRedirect)
      case None => Future(Ok(views.html.register(registerForm)))
    }
  }

  def doRegister() = silhouette.UnsecuredAction.async { implicit req =>
    registerForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.register(formWithErrors))),
      formData => {
        val result = Redirect(routes.AuthCtrl.register()).flashing("info" -> s"You're almost done! We sent an activation mail to ${formData.email}. Please follow the instructions in the email to activate your account. If it doesn't arrive, check your spam folder, or try to log in again to send another activation mail.")
        val loginInfo = LoginInfo(CredentialsProvider.ID, formData.email)
        userRepository.retrieve(loginInfo).flatMap {
          case Some(user) =>
            mailerSrv.sendAlreadyRegistered(formData.email, user)
            Future(result)
          case None =>
            val authInfo = passwordHasherRegistry.current.hash(formData.password)
            val user = User.from(formData, loginInfo)
            for {
              avatar <- avatarService.retrieveURL(formData.email)
              user <- userRepository.create(user.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authToken <- authTokenRepository.create(AuthToken.from(user.id))
            } yield {
              mailerSrv.sendRegister(formData.email, user, authToken)
              silhouette.env.eventBus.publish(SignUpEvent(user, req))
              result
            }
        }
      }
    )
  }

  def login() = silhouette.UserAwareAction.async { implicit req: UserAwareRequest[SilhouetteEnv, AnyContent] =>
    req.identity match {
      case Some(user) => Future(loginRedirect)
      case None => Future(Ok(views.html.login(loginForm)))
    }
  }

  def doLogin() = silhouette.UnsecuredAction.async { implicit req =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.login(formWithErrors))),
      formData => {
        val credentials = Credentials(formData.email, formData.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          userRepository.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated => Future(Ok(views.html.activateAccount(formData.email)))
            case Some(user) =>
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator: CookieAuthenticator if formData.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = DateTime.now.withDurationAdded(conf.RememberMe.expiry.toMillis, 1),
                    idleTimeout = conf.RememberMe.idleTimeout,
                    cookieMaxAge = conf.RememberMe.cookieMaxAge
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, req))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, loginRedirect)
                }
              }
            case None =>
              credentialsRepository.remove(loginInfo).map { _ =>
                Redirect(routes.AuthCtrl.login()).flashing("error" -> "Unable to find corresponding user, credentials removed.")
              }
          }
        }.recover {
          case e: ProviderException => Redirect(routes.AuthCtrl.login()).flashing("error" -> "Invalid credentials!")
        }
      }
    )
  }

  def doLogout() = silhouette.UserAwareAction.async { implicit req =>
    (req.identity, req.authenticator) match {
      case (Some(user), Some(authenticator)) =>
        silhouette.env.eventBus.publish(LogoutEvent(user, req))
        silhouette.env.authenticatorService.discard(authenticator, logoutRedirect)
      case _ => Future(logoutRedirect)
    }
  }

  // TODO : use it & refactor
  def sendActivationEmail(email: String) = silhouette.UnsecuredAction.async { implicit req =>
    val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.AuthCtrl.login()).flashing("info" -> s"We sent another activation email to you at <b>$decodedEmail</b>. It might take a few minutes for it to arrive; be sure to check your spam folder.")
    userRepository.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenRepository.create(AuthToken.from(user.id)).map { authToken =>
          mailerSrv.sendActivateAccount(decodedEmail, user, authToken)
          result
        }
      case None => Future(result)
    }
  }

  def activateAccount(id: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit req =>
    authTokenRepository.get(id).flatMap {
      case Some(authToken) => userRepository.get(authToken.userId).flatMap {
        case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
          userRepository.activate(user.id).map { _ =>
            Redirect(routes.AuthCtrl.login()).flashing("success" -> "Your account is now activated! Please login to use your new account.")
          }
        case _ => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please login to send the activation email again."))
      }
      case None => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please login to send the activation email again."))
    }
  }

  def debug = silhouette.UserAwareAction.async { implicit req =>
    for {
      users <- userRepository.find()
      authTokens <- authTokenRepository.find()
      credentials <- credentialsRepository.find()
    } yield Ok(views.html.debug(users, credentials, authTokens, req.identity))
  }
  def debugRemoveUser(id: User.Id) = Action.async { implicit req =>
    userRepository.get(id).flatMap { userOpt =>
      userOpt.map { user =>
        for {
          a <- authTokenRepository.delete(id)
          c <- credentialsRepository.remove(user.loginInfo)
          u <- userRepository.delete(id)
        } yield Redirect(routes.AuthCtrl.debug())
      }.getOrElse {
        Future(Redirect(routes.AuthCtrl.debug()))
      }
    }
  }
  def debugRemoveToken(id: AuthToken.Id) = Action.async { implicit req =>
    authTokenRepository.delete(id).map { _ =>
      Redirect(routes.AuthCtrl.debug())
    }
  }
}
