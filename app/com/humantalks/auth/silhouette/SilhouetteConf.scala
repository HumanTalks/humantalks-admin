package com.humantalks.auth.silhouette

import play.api.Configuration
import net.ceedubs.ficus.Ficus._
import scala.concurrent.duration._

case class SilhouetteConf(configuration: Configuration) {
  object RememberMe {
    val conf = configuration.getConfig("silhouette.authenticator.rememberMe")
    val expiry: FiniteDuration = conf.map(_.underlying.as[FiniteDuration]("authenticatorExpiry")).getOrElse(30.days)
    val idleTimeout: Option[FiniteDuration] = conf.flatMap(_.underlying.getAs[FiniteDuration]("authenticatorIdleTimeout"))
    val cookieMaxAge: Option[FiniteDuration] = conf.flatMap(_.underlying.getAs[FiniteDuration]("cookieMaxAge"))
  }
}
