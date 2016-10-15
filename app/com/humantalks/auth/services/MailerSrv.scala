package com.humantalks.auth.services

import com.humantalks.auth.entities.{ AuthToken, User }
import com.humantalks.auth.{ views, routes }
import play.api.i18n.MessagesApi
import play.api.libs.mailer.{ Email, MailerClient }
import play.api.mvc.RequestHeader

case class MailerSrv(mailerClient: MailerClient) {
  val emailFrom = "HumanTalks <paris@humantalks.com>"

  def sendRegister(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): String = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    mailerClient.send(Email(
      subject = "Welcome",
      from = emailFrom,
      to = Seq(email),
      bodyText = Some(views.txt.emails.register(user, url).body),
      bodyHtml = Some(views.html.emails.register(user, url).body)
    ))
  }

  def sendAlreadyRegistered(email: String, user: User)(implicit request: RequestHeader, messagesApi: MessagesApi): String = {
    val url = routes.AuthCtrl.login().absoluteURL()
    mailerClient.send(Email(
      subject = "Welcome",
      from = emailFrom,
      to = Seq(email),
      bodyText = Some(views.txt.emails.alreadyRegistered(user, url).body),
      bodyHtml = Some(views.html.emails.alreadyRegistered(user, url).body)
    ))
  }

  def sendActivateAccount(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): String = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    mailerClient.send(Email(
      subject = "Activate account",
      from = emailFrom,
      to = Seq(email),
      bodyText = Some(views.txt.emails.activateAccount(user, url).body),
      bodyHtml = Some(views.html.emails.activateAccount(user, url).body)
    ))
  }

  def sentResetPassword(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): String = {
    val url = routes.AuthCtrl.resetPassword(authToken.id).absoluteURL()
    mailerClient.send(Email(
      subject = "Reset password",
      from = emailFrom,
      to = Seq(email),
      bodyText = Some(views.txt.emails.resetPassword(user, url).body),
      bodyHtml = Some(views.html.emails.resetPassword(user, url).body)
    ))
  }
}
