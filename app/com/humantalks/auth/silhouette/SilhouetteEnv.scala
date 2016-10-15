package com.humantalks.auth.silhouette

import com.humantalks.auth.entities.User
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

trait SilhouetteEnv extends Env {
  type I = User
  type A = CookieAuthenticator
}
