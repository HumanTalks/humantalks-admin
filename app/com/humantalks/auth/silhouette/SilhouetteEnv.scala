package com.humantalks.auth.silhouette

import com.humantalks.internal.persons.Person
import com.mohiva.play.silhouette.api.Env
import com.mohiva.play.silhouette.impl.authenticators.CookieAuthenticator

trait SilhouetteEnv extends Env {
  type I = Person
  type A = CookieAuthenticator
}
