package com.humantalks.auth.services

import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.{ AuthInfo, LoginInfo }
import com.mohiva.play.silhouette.api.util.{ Credentials, PasswordInfo, PasswordHasherRegistry }
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider

import scala.concurrent.Future

case class AuthSrv(
    passwordHasherRegistry: PasswordHasherRegistry,
    credentialsProvider: CredentialsProvider,
    authInfoRepository: AuthInfoRepository
) {
  def hashPassword(password: String): PasswordInfo = passwordHasherRegistry.current.hash(password)
  def authenticate(email: String, password: String): Future[LoginInfo] = credentialsProvider.authenticate(Credentials(email, password))
  def createAuthInfo[T <: AuthInfo](loginInfo: LoginInfo, authInfo: T): Future[T] = authInfoRepository.add(loginInfo, authInfo)
  def updateAuthInfo[T <: AuthInfo](loginInfo: LoginInfo, authInfo: T): Future[T] = authInfoRepository.update(loginInfo, authInfo)
}
