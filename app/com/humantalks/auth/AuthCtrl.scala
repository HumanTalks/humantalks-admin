package com.humantalks.auth

import java.net.URLDecoder

import com.humantalks.auth.silhouette.forms.{ Login, Register }
import com.humantalks.auth.silhouette._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.{ LoginEvent, SignUpEvent, LoginInfo, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{ Clock, Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{ SocialProviderRegistry, CredentialsProvider }
import global.Contexts
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.{ I18nSupport, MessagesApi }
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc.Controller

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class AuthCtrl(
    configuration: Configuration,
    ctx: Contexts,
    silhouette: Silhouette[DefaultEnv],
    userService: UserService,
    authInfoRepository: AuthInfoRepository,
    authTokenService: AuthTokenService,
    avatarService: AvatarService,
    passwordHasherRegistry: PasswordHasherRegistry,
    credentialsProvider: CredentialsProvider,
    socialProviderRegistry: SocialProviderRegistry,
    mailerClient: MailerClient,
    clock: Clock
)(implicit messagesApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val registerForm = Form(Register.fields)
  val loginForm = Form(Login.fields)

  def register = silhouette.UnsecuredAction.async { implicit request =>
    Future(Ok(views.html.register(registerForm)))
  }

  /*def doRegister = silhouette.UnsecuredAction.async { implicit request =>
    registerForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.register(formWithErrors))),
      register => {
        val result = Redirect(routes.AuthCtrl.register()).flashing("info" -> messagesApi("sign.up.email.sent", register.email))
        val loginInfo = LoginInfo(CredentialsProvider.ID, register.email)
        userService.retrieve(loginInfo).flatMap {
          case Some(user) => {
            val url = routes.AuthCtrl.login().absoluteURL()
            mailerClient.send(Email(
              subject = messagesApi("email.already.signed.up.subject"),
              from = messagesApi("email.from"),
              to = Seq(register.email),
              bodyText = Some(views.txt.emails.alreadyRegistered(user, url).body),
              bodyHtml = Some(views.html.emails.alreadyRegistered(user, url).body)
            ))

            Future(result)
          }
          case None => {
            val authInfo = passwordHasherRegistry.current.hash(register.password)
            val user = User.from(register, loginInfo)
            for {
              avatar <- avatarService.retrieveURL(register.email)
              user <- userService.save(user.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authToken <- authTokenService.create(user.id)
            } yield {
              val url = routes.AuthCtrl.activate(authToken.id).absoluteURL()
              mailerClient.send(Email(
                subject = messagesApi("email.sign.up.subject"),
                from = messagesApi("email.from"),
                to = Seq(register.email),
                bodyText = Some(views.txt.emails.signUp(user, url).body),
                bodyHtml = Some(views.html.emails.signUp(user, url).body)
              ))

              silhouette.env.eventBus.publish(SignUpEvent(user, request))
              result
            }
          }
        }
      }
    )
  }*/

  def login = silhouette.UnsecuredAction.async { implicit request =>
    Future(Ok(views.html.login(loginForm, socialProviderRegistry)))
  }

  /*def doLogin = silhouette.UnsecuredAction.async { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.login(formWithErrors, socialProviderRegistry))),
      login => {
        val credentials = Credentials(login.email, login.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(com.humantalks.common.controllers.routes.Application.index())
          userService.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated => Future(Ok(views.html.activateAccount(login.email)))
            case Some(user) => {
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator if login.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = clock.now + c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry"),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, request))
                silhouette.env.authenticatorService.init(authenticator).flatMap { v =>
                  silhouette.env.authenticatorService.embed(v, result)
                }
              }
            }
            case None => Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }.recover {
          case e: ProviderException => Redirect(routes.AuthCtrl.login()).flashing("error" -> messagesApi("invalid.credentials"))
        }
      }
    )
  }*/

  /*def activationEmail(email: String) = silhouette.UnsecuredAction.async { implicit request =>
    val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.AuthCtrl.login()).flashing("info" -> messagesApi("activation.email.sent", decodedEmail))

    userService.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenService.create(user.id).map { authToken =>
          val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()

          mailerClient.send(Email(
            subject = messagesApi("email.activate.account.subject"),
            from = messagesApi("email.from"),
            to = Seq(decodedEmail),
            bodyText = Some(views.txt.emails.activateAccount(user, url).body),
            bodyHtml = Some(views.html.emails.activateAccount(user, url).body)
          ))
          result
        }
      case None => Future(result)
    }
  }*/

  /*def activateAccount(id: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit request =>
    authTokenService.validate(id).flatMap {
      case Some(authToken) => userService.retrieve(authToken.userId).flatMap {
        case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
          userService.save(user.copy(activated = true)).map { _ =>
            Redirect(routes.AuthCtrl.login()).flashing("success" -> messagesApi("account.activated"))
          }
        case _ => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> messagesApi("invalid.activation.link")))
      }
      case None => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> messagesApi("invalid.activation.link")))
    }
  }*/
}
