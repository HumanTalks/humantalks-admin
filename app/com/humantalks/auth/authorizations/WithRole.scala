package com.humantalks.auth.authorizations

import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Authorization
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator
import play.api.mvc.Request

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

case class WithRole(role: Person.Role.Value) extends Authorization[Person, CookieAuthenticator] {
  def isAuthorized[B](person: Person, authenticator: CookieAuthenticator)(implicit request: Request[B]): Future[Boolean] = {
    Future(person.role.exists(_ >= role))
  }
}
