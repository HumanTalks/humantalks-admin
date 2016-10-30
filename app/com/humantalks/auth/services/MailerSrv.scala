package com.humantalks.auth.services

import com.humantalks.auth.entities.AuthToken
import com.humantalks.auth.{ views, routes }
import com.humantalks.common.Conf
import com.humantalks.common.services.sendgrid._
import com.humantalks.internal.persons.Person
import play.api.i18n.MessagesApi
import play.api.libs.ws.WSResponse
import play.api.mvc.RequestHeader

import scala.concurrent.Future

case class MailerSrv(conf: Conf, sendgridSrv: SendgridSrv) {
  val from = Address(conf.Organization.Admin.email, Some(conf.Organization.Admin.name))

  def sendRegister(email: String, person: Person, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = messagesApi("auth.email.register.subject"),
      content = Seq(
        Content.text(views.txt.emails.register(person, url).body),
        Content.html(views.html.emails.register(person, url).body)
      )
    ))
  }

  def sendAlreadyRegistered(email: String, person: Person)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.login().absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = messagesApi("auth.email.already_registered.subject"),
      content = Seq(
        Content.text(views.txt.emails.alreadyRegistered(person, url).body),
        Content.html(views.html.emails.alreadyRegistered(person, url).body)
      )
    ))
  }

  def sendActivateAccount(email: String, person: Person, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.activateAccount(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = messagesApi("auth.email.activate_account.subject"),
      content = Seq(
        Content.text(views.txt.emails.activateAccount(person, url).body),
        Content.html(views.html.emails.activateAccount(person, url).body)
      )
    ))
  }

  def sentResetPassword(email: String, person: Person, authToken: AuthToken)(implicit request: RequestHeader, messagesApi: MessagesApi): Future[WSResponse] = {
    val url = routes.AuthCtrl.resetPassword(authToken.id).absoluteURL()
    sendgridSrv.send(Email(
      personalizations = Recipient.single(email),
      from = from,
      subject = messagesApi("auth.email.reset_password.subject"),
      content = Seq(
        Content.text(views.txt.emails.resetPassword(person, url).body),
        Content.html(views.html.emails.resetPassword(person, url).body)
      )
    ))
  }
}
