package com.humantalks.common

import com.humantalks.meetups.Meetup
import com.humantalks.persons.Person
import com.humantalks.talks.Talk
import com.humantalks.venues.Venue
import global.infrastructure.Repository
import play.api.Configuration
import play.api.i18n.DefaultLangs

case class Conf(configuration: Configuration) {
  object App {
    val langs = new DefaultLangs(configuration).availables
  }
  object Repositories {
    val person = Repository.Collection[Person]("Person")
    val venue = Repository.Collection[Venue]("Venue")
    val talk = Repository.Collection[Talk]("Talk")
    val meetup = Repository.Collection[Meetup]("Meetup")
  }
}
