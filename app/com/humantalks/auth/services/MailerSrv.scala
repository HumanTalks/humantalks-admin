package com.humantalks.auth.services

import com.humantalks.auth.entities.{ AuthToken, User }
import com.humantalks.auth.{ views, routes }
import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSResponse
import play.api.mvc.RequestHeader

import scala.concurrent.Future

case class MailerSrv(conf: Conf, sendgridSrv: SendgridSrv) {
  val from = Address(conf.Sendgrid.senderEmail, conf.Sendgrid.senderName)

  def sendRegister(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = "Welcome",
      content = Seq(
        Content.text(views.txt.emails.register(user, url).body),
        Content.html(views.html.emails.register(user, url).body)
      )
    ))
  }

  def sendAlreadyRegistered(email: String, user: User)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.login().absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = "Welcome",
      content = Seq(
        Content.text(views.txt.emails.alreadyRegistered(user, url).body),
        Content.html(views.html.emails.alreadyRegistered(user, url).body)
      )
    ))
  }

  def sendActivateAccount(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = "Activate account",
      content = Seq(
        Content.text(views.txt.emails.activateAccount(user, url).body),
        Content.html(views.html.emails.activateAccount(user, url).body)
      )
    ))
  }

  def sentResetPassword(email: String, user: User, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.resetPassword(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = "Reset password",
      content = Seq(
        Content.text(views.txt.emails.resetPassword(user, url).body),
        Content.html(views.html.emails.resetPassword(user, url).body)
      )
    ))
  }
}
