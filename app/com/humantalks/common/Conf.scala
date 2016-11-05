package com.humantalks.common

import com.humantalks.auth.entities.{ Credentials, AuthToken }
import com.humantalks.exposed.proposals.Proposal
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import global.infrastructure.Repository
import net.ceedubs.ficus.Ficus._
import play.api.Configuration
import play.api.i18n.DefaultLangs

import scala.concurrent.duration._

case class Conf(configuration: Configuration) {
  object App {
    val env = configuration.getString("application.env").get
    val langs = new DefaultLangs(configuration).availables
    def isProd: Boolean = env == "prod"
  }
  object Organization {
    object Admin {
      val conf = configuration.getConfig("organization.admin").get
      val name = conf.getString("name").get
      val email = conf.getString("email").get
    }
  }
  object Repositories {
    val credentials = Repository.Collection[Credentials]("Credentials")
    val authToken = Repository.Collection[AuthToken]("AuthToken")
    val person = Repository.Collection[Person]("Person")
    val venue = Repository.Collection[Venue]("Venue")
    val talk = Repository.Collection[Talk]("Talk")
    val meetup = Repository.Collection[Meetup]("Meetup")
    val proposal = Repository.Collection[Proposal]("Proposal")
  }
  object Auth {
    object RememberMe {
      val conf = configuration.getConfig("silhouette.authenticator.rememberMe")
      val expiry: FiniteDuration = conf.map(_.underlying.as[FiniteDuration]("authenticatorExpiry")).getOrElse(30.days)
      val idleTimeout: Option[FiniteDuration] = conf.flatMap(_.underlying.getAs[FiniteDuration]("authenticatorIdleTimeout"))
      val cookieMaxAge: Option[FiniteDuration] = conf.flatMap(_.underlying.getAs[FiniteDuration]("cookieMaxAge"))
    }
  }
  object Sendgrid {
    val conf = configuration.getConfig("sendgrid").get
    val apiKey: String = conf.getString("api-key").get
  }
  object Slack {
    val conf = configuration.getConfig("slack").get
    val token: String = conf.getString("token").get
    val botName: String = conf.getString("botName").get
    val botIcon: String = conf.getString("botIcon").get
    val proposalChannel: String = conf.getString("proposalChannel").get
  }
}
