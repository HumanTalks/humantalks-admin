package com.humantalks.auth.silhouette

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.services.IdentityService
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile

import scala.concurrent.{ ExecutionContext, Future }

case class UserService(userRepository: UserRepository) extends IdentityService[User] {
  def retrieve(id: User.Id): Future[Option[User]] = userRepository.get(id)

  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userRepository.get(loginInfo)

  def save(user: User): Future[User] = userRepository.upsert(user)

  def save(profile: CommonSocialProfile)(implicit ec: ExecutionContext): Future[User] =
    userRepository.get(profile.loginInfo).flatMap {
      case Some(user) => userRepository.create(user.merge(profile))
      case None => userRepository.update(User.from(profile))
    }
}
