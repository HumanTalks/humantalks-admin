package com.humantalks.auth.silhouette

import com.mohiva.play.silhouette.api.util.Clock
import org.joda.time.DateTimeZone

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration._

case class AuthTokenService(authTokenRepository: AuthTokenRepository, clock: Clock) {
  def create(userId: User.Id, expiry: FiniteDuration = 5.minutes): Future[AuthToken] = {
    val token = AuthToken(AuthToken.Id.generate(), userId, clock.now.withZone(DateTimeZone.UTC).plusSeconds(expiry.toSeconds.toInt))
    authTokenRepository.create(token)
  }

  def validate(id: AuthToken.Id): Future[Option[AuthToken]] =
    authTokenRepository.get(id)

  def clean(implicit ec: ExecutionContext): Future[Seq[AuthToken]] =
    authTokenRepository.findExpired(clock.now.withZone(DateTimeZone.UTC)).flatMap { tokens =>
      Future.sequence(tokens.map { token =>
        authTokenRepository.delete(token.id).map(_ => token)
      })
    }
}
