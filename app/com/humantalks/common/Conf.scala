package com.humantalks.common

import com.humantalks.domain.{ Talk, Meetup, Venue, Person }
import global.infrastructure.GenericRepository
import play.api.Configuration
import play.api.i18n.DefaultLangs

case class Conf(configuration: Configuration) {
  object App {
    val langs = new DefaultLangs(configuration).availables
  }
  object Repositories {
    val person = GenericRepository.Collection[Person]("Person")
    val venue = GenericRepository.Collection[Venue]("Venue")
    val talk = GenericRepository.Collection[Talk]("Talk")
    val meetup = GenericRepository.Collection[Meetup]("Meetup")
  }
}
