package com.humantalks.auth

import java.net.URLDecoder

import com.humantalks.auth.entities.AuthToken
import com.humantalks.auth.infrastructure.{ CredentialsRepository, AuthTokenRepository }
import com.humantalks.auth.forms._
import com.humantalks.auth.services.{ AuthSrv, MailerSrv }
import com.humantalks.auth.silhouette._
import com.humantalks.common.Conf
import com.humantalks.internal.persons.PersonRepository
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ConfigurationException
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.{ InvalidPasswordException, IdentityNotFoundException }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import global.Contexts
import org.joda.time.DateTime
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.Controller

import scala.concurrent.Future

case class AuthCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    conf: Conf,
    authSrv: AuthSrv,
    personRepository: PersonRepository,
    credentialsRepository: CredentialsRepository,
    authTokenRepository: AuthTokenRepository,
    avatarService: AvatarService,
    mailerSrv: MailerSrv
)(implicit messagesApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val registerForm = Form(RegisterForm.fields)
  val loginForm = Form(LoginForm.fields)
  val forgotPasswordForm = Form(ForgotPasswordForm.fields)
  val resetPasswordForm = Form(ResetPasswordForm.fields)
  val loginRedirect = Redirect(com.humantalks.internal.routes.Application.index())
  val logoutRedirect = Redirect(com.humantalks.exposed.routes.Application.index())

  def register() = silhouette.UserAwareAction.async { implicit req =>
    req.identity match {
      case Some(user) => Future.successful(loginRedirect)
      case None => Future.successful(Ok(views.html.register(registerForm)))
    }
  }

  def doRegister() = silhouette.UnsecuredAction.async { implicit req =>
    registerForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.register(formWithErrors))),
      formData => {
        val result = Redirect(routes.AuthCtrl.register()).flashing("info" -> s"You're almost done! We sent an activation mail to <b>${formData.email}</b>. Please follow the instructions in the email to activate your account. If it doesn't arrive, check your spam folder, or try to log in again to send another activation mail.")
        val loginInfo = LoginInfo(CredentialsProvider.ID, formData.email)
        personRepository.retrieve(loginInfo).flatMap {
          case Some(user) =>
            mailerSrv.sendAlreadyRegistered(formData.email, user)
            Future.successful(result)
          case None =>
            val passwordInfo = authSrv.hashPassword(formData.password)
            for {
              avatar <- avatarService.retrieveURL(formData.email)
              person <- personRepository.register(formData, loginInfo, avatar)
              authInfo <- authSrv.createAuthInfo(loginInfo, passwordInfo)
              authToken <- authTokenRepository.create(person.id)
            } yield {
              mailerSrv.sendRegister(formData.email, person, authToken)
              silhouette.env.eventBus.publish(SignUpEvent(person, req))
              result
            }
        }
      }
    )
  }

  def login() = silhouette.UserAwareAction.async { implicit req =>
    req.identity match {
      case Some(person) => Future.successful(loginRedirect)
      case None => Future.successful(Ok(views.html.login(loginForm)))
    }
  }

  def doLogin() = silhouette.UnsecuredAction.async { implicit req =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.login(formWithErrors))),
      formData => {
        authSrv.authenticate(formData.email, formData.password).flatMap { loginInfo =>
          personRepository.retrieve(loginInfo).flatMap {
            case Some(person) if !person.activated => Future.successful(Ok(views.html.activateAccount(formData.email)))
            case Some(person) =>
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator: CookieAuthenticator if formData.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = DateTime.now.withDurationAdded(conf.Auth.RememberMe.expiry.toMillis, 1),
                    idleTimeout = conf.Auth.RememberMe.idleTimeout,
                    cookieMaxAge = conf.Auth.RememberMe.cookieMaxAge
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(person, req))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, loginRedirect)
                }
              }
            case None =>
              credentialsRepository.remove(loginInfo).map { _ =>
                Redirect(routes.AuthCtrl.login()).flashing("error" -> "Unable to find corresponding person, credentials removed")
              }
          }
        } recover {
          case e: InvalidPasswordException => Redirect(routes.AuthCtrl.login()).flashing("error" -> "Invalid password")
          case e: ConfigurationException => Redirect(routes.AuthCtrl.login()).flashing("error" -> "ConfigurationException")
          case e: IdentityNotFoundException => Redirect(routes.AuthCtrl.login()).flashing("error" -> "Unable to find credentials")
        }
      }
    )
  }

  def doLogout() = silhouette.UserAwareAction.async { implicit req =>
    (req.identity, req.authenticator) match {
      case (Some(person), Some(authenticator)) =>
        silhouette.env.eventBus.publish(LogoutEvent(person, req))
        silhouette.env.authenticatorService.discard(authenticator, logoutRedirect)
      case _ => Future.successful(logoutRedirect)
    }
  }

  // TODO : use it & refactor
  def sendActivationEmail(email: String) = silhouette.UnsecuredAction.async { implicit req =>
    val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.AuthCtrl.login()).flashing("info" -> s"We sent another activation email to you at <b>$decodedEmail</b>. It might take a few minutes for it to arrive; be sure to check your spam folder.")
    personRepository.retrieve(loginInfo).flatMap {
      case Some(person) if !person.activated =>
        authTokenRepository.create(person.id).map { authToken =>
          mailerSrv.sendActivateAccount(decodedEmail, person, authToken)
          result
        }
      case None => Future.successful(result)
    }
  }

  def activateAccount(id: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit req =>
    authTokenRepository.get(id).flatMap {
      case Some(authToken) => personRepository.get(authToken.person).flatMap {
        case Some(person) if person.hasProvider(CredentialsProvider.ID) =>
          personRepository.activate(person.id).map { _ =>
            Redirect(routes.AuthCtrl.login()).flashing("success" -> "Your account is now activated! Please login to use your new account.")
          }
        case _ => Future.successful(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please login to send the activation email again."))
      }
      case None => Future.successful(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please login to send the activation email again."))
    }
  }

  def forgotPassword() = silhouette.UnsecuredAction.async { implicit req =>
    Future.successful(Ok(views.html.forgotPassword(forgotPasswordForm)))
  }

  def doForgotPassword() = silhouette.UnsecuredAction.async { implicit req =>
    forgotPasswordForm.bindFromRequest.fold(
      formWithErrors => Future.successful(BadRequest(views.html.forgotPassword(formWithErrors))),
      formData => {
        val result = Redirect(routes.AuthCtrl.login()).flashing("info" -> "We have sent you an email with further instructions to reset your password, on condition that the address was found in our system. If you do not receive an email within the next 5 minutes, then please recheck your entered email address and try it again.")
        val loginInfo = LoginInfo(CredentialsProvider.ID, formData.email)
        personRepository.retrieve(loginInfo).flatMap {
          case Some(person) if person.data.email.isDefined =>
            authTokenRepository.create(person.id).flatMap { authToken =>
              mailerSrv.sentResetPassword(formData.email, person, authToken).map { res =>
                if (res.status == 202) {
                  result
                } else {
                  Redirect(routes.AuthCtrl.forgotPassword()).flashing("error" -> s"<b>${res.status} ${res.statusText}</b> ${res.body}")
                }
              }
            }
          case None =>
            Future.successful(result)
        }
      }
    )
  }

  def resetPassword(token: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit req =>
    authTokenRepository.get(token).map {
      case Some(authToken) => Ok(views.html.resetPassword(resetPasswordForm, token))
      case None => Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please request a new link to reset your password.")
    }
  }

  def doResetPassword(token: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit req =>
    authTokenRepository.get(token).flatMap {
      case Some(authToken) =>
        resetPasswordForm.bindFromRequest.fold(
          formWithErrors => Future.successful(BadRequest(views.html.resetPassword(formWithErrors, token))),
          formData => personRepository.get(authToken.person).flatMap {
            case Some(person) if person.hasProvider(CredentialsProvider.ID) =>
              val passwordInfo = authSrv.hashPassword(formData.password)
              authSrv.updateAuthInfo(person.loginInfo.get, passwordInfo).map { _ =>
                Redirect(routes.AuthCtrl.login()).flashing("success" -> "Mot de passe réinitialisé")
              }
            case _ => Future.successful(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please request a new link to reset your password. (no user)"))
          }
        )
      case None => Future.successful(Redirect(routes.AuthCtrl.login()).flashing("error" -> "The link isn't valid anymore! Please request a new link to reset your password. (no token)"))
    }
  }

  /*def debug = silhouette.UserAwareAction.async { implicit req =>
    for {
      persons <- personRepository.findUsers()
      authTokens <- authTokenRepository.find()
      credentials <- credentialsRepository.find()
    } yield Ok(views.html.debug(persons, credentials, authTokens, req.identity))
  }
  def debugRemoveUser(id: Person.Id) = Action.async { implicit req =>
    personRepository.get(id).flatMap { personOpt =>
      personOpt.map { person =>
        for {
          a <- authTokenRepository.delete(id)
          c <- credentialsRepository.remove(person.loginInfo.get)
          u <- personRepository.unregister(id)
        } yield Redirect(routes.AuthCtrl.debug())
      }.getOrElse {
        Future.successful(Redirect(routes.AuthCtrl.debug()))
      }
    }
  }
  def debugRemoveToken(id: AuthToken.Id) = Action.async { implicit req =>
    authTokenRepository.delete(id).map { _ =>
      Redirect(routes.AuthCtrl.debug())
    }
  }*/
}
