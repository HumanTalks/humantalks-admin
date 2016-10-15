package com.humantalks.auth

import java.net.URLDecoder

import com.humantalks.auth.entities.{ AuthToken, User }
import com.humantalks.auth.infrastructure.{ UserRepository, CredentialsRepository, AuthTokenRepository }
import com.humantalks.auth.forms.{ Login, Register }
import com.humantalks.auth.silhouette._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.api.{ LoginEvent, SignUpEvent, LoginInfo, Silhouette }
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AvatarService
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordHasherRegistry }
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.{ SocialProviderRegistry, CredentialsProvider }
import global.Contexts
import net.ceedubs.ficus.Ficus._
import org.joda.time.DateTime
import play.api.Configuration
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc.{ AnyContent, Request, Action, Controller }

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class AuthCtrl(
    configuration: Configuration,
    ctx: Contexts,
    userRepository: UserRepository,
    credentialsRepository: CredentialsRepository,
    authTokenRepository: AuthTokenRepository,
    silhouette: Silhouette[DefaultEnv],
    passwordHasherRegistry: PasswordHasherRegistry,
    avatarService: AvatarService,
    authInfoRepository: AuthInfoRepository,
    credentialsProvider: CredentialsProvider,
    socialProviderRegistry: SocialProviderRegistry,
    mailerClient: MailerClient
)(implicit messagesApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val registerForm = Form(Register.fields)
  val loginForm = Form(Login.fields)

  def register() = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    Future(Ok(views.html.register(registerForm)))
  }

  def doRegister() = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    registerForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.register(formWithErrors))),
      register => {
        val result = Redirect(routes.AuthCtrl.register()).flashing("info" -> messagesApi("sign.up.email.sent", register.email))
        val loginInfo = LoginInfo(CredentialsProvider.ID, register.email)
        userRepository.retrieve(loginInfo).flatMap {
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
              user <- userRepository.create(user.copy(avatarURL = avatar))
              authInfo <- authInfoRepository.add(loginInfo, authInfo)
              authToken <- authTokenRepository.create(AuthToken.from(user.id))
            } yield {
              val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
              mailerClient.send(Email(
                subject = messagesApi("email.sign.up.subject"),
                from = messagesApi("email.from"),
                to = Seq(register.email),
                bodyText = Some(views.txt.emails.register(user, url).body),
                bodyHtml = Some(views.html.emails.register(user, url).body)
              ))

              silhouette.env.eventBus.publish(SignUpEvent(user, req))
              result
            }
          }
        }
      }
    )
  }

  def login() = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    Future(Ok(views.html.login(loginForm, socialProviderRegistry)))
  }

  def doLogin() = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    loginForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(views.html.login(formWithErrors, socialProviderRegistry))),
      login => {
        val credentials = Credentials(login.email, login.password)
        credentialsProvider.authenticate(credentials).flatMap { loginInfo =>
          val result = Redirect(com.humantalks.internal.routes.Application.index())
          userRepository.retrieve(loginInfo).flatMap {
            case Some(user) if !user.activated => Future(Ok(views.html.activateAccount(login.email)))
            case Some(user) => {
              val c = configuration.underlying
              silhouette.env.authenticatorService.create(loginInfo).map {
                case authenticator: CookieAuthenticator if login.rememberMe =>
                  authenticator.copy(
                    expirationDateTime = DateTime.now.withDurationAdded(c.as[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorExpiry").toMillis, 1),
                    idleTimeout = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.authenticatorIdleTimeout"),
                    cookieMaxAge = c.getAs[FiniteDuration]("silhouette.authenticator.rememberMe.cookieMaxAge")
                  )
                case authenticator => authenticator
              }.flatMap { authenticator =>
                silhouette.env.eventBus.publish(LoginEvent(user, req))
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
  }

  def activationEmail(email: String) = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    val decodedEmail = URLDecoder.decode(email, "UTF-8")
    val loginInfo = LoginInfo(CredentialsProvider.ID, decodedEmail)
    val result = Redirect(routes.AuthCtrl.login()).flashing("info" -> messagesApi("activation.email.sent", decodedEmail))

    userRepository.retrieve(loginInfo).flatMap {
      case Some(user) if !user.activated =>
        authTokenRepository.create(AuthToken.from(user.id)).map { authToken =>
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
  }

  def activateAccount(id: AuthToken.Id) = silhouette.UnsecuredAction.async { implicit req: Request[AnyContent] =>
    authTokenRepository.get(id).flatMap {
      case Some(authToken) => userRepository.get(authToken.userId).flatMap {
        case Some(user) if user.loginInfo.providerID == CredentialsProvider.ID =>
          userRepository.update(user.copy(activated = true)).map { _ =>
            Redirect(routes.AuthCtrl.login()).flashing("success" -> messagesApi("account.activated"))
          }
        case _ => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> messagesApi("invalid.activation.link")))
      }
      case None => Future(Redirect(routes.AuthCtrl.login()).flashing("error" -> messagesApi("invalid.activation.link")))
    }
  }

  def debug = Action.async { implicit req: Request[AnyContent] =>
    for {
      users <- userRepository.find()
      authTokens <- authTokenRepository.find()
      credentials <- credentialsRepository.find()
    } yield Ok(views.html.debug(users, credentials, authTokens))
  }
  def debugRemoveUser(id: User.Id) = Action.async { implicit req: Request[AnyContent] =>
    userRepository.get(id).flatMap { userOpt =>
      userOpt.map { user =>
        for {
          a <- authTokenRepository.delete(id)
          u <- userRepository.delete(id)
          c <- credentialsRepository.remove(user.loginInfo)
        } yield Redirect(routes.AuthCtrl.debug())
      }.getOrElse {
        Future(Redirect(routes.AuthCtrl.debug()))
      }
    }
  }
  def debugRemoveToken(id: AuthToken.Id) = Action.async { implicit req: Request[AnyContent] =>
    authTokenRepository.delete(id).map { _ =>
      Redirect(routes.AuthCtrl.debug())
    }
  }
}
