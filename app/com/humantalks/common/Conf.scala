package com.humantalks.common

import com.humantalks.auth.silhouette.{ Credentials, AuthToken, User }
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.infrastructure.Repository
import play.api.Configuration
import play.api.i18n.DefaultLangs

case class Conf(configuration: Configuration) {
  object App {
    val langs = new DefaultLangs(configuration).availables
  }
  object Repositories {
    val user = Repository.Collection[User]("User")
    val credentials = Repository.Collection[Credentials]("Credentials")
    val authToken = Repository.Collection[AuthToken]("AuthToken")
    val person = Repository.Collection[Person]("Person")
    val venue = Repository.Collection[Venue]("Venue")
    val talk = Repository.Collection[Talk]("Talk")
    val meetup = Repository.Collection[Meetup]("Meetup")
  }
}
