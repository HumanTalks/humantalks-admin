package com.humantalks.auth.helpers

import com.humantalks.auth.silhouette.{ User, DefaultEnv }
import com.mohiva.play.silhouette.api.actions.{ UserAwareRequest, SecuredRequest }
import com.mohiva.play.silhouette.api.{ Environment, Silhouette }
import play.api.i18n.I18nSupport
import play.api.mvc.Controller

trait AuthController extends Controller with I18nSupport {
  def silhouette: Silhouette[DefaultEnv]
  def env: Environment[DefaultEnv] = silhouette.env

  def SecuredAction = silhouette.SecuredAction
  def UnsecuredAction = silhouette.UnsecuredAction
  def UserAwareAction = silhouette.UserAwareAction

  implicit def securedRequest2User[A](implicit request: SecuredRequest[DefaultEnv, A]): User = request.identity
  implicit def userAwareRequest2UserOpt[A](implicit request: UserAwareRequest[DefaultEnv, A]): Option[User] = request.identity
}
