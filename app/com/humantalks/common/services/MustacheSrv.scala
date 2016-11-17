package com.humantalks.common.services

import java.io.{ StringReader, StringWriter }
import com.humantalks.exposed.entities.{ PublicTalk, PublicVenue, PublicMeetup }
import com.humantalks.internal.meetups.Meetup
import com.humantalks.internal.persons.Person
import com.humantalks.internal.talks.Talk
import com.humantalks.internal.venues.Venue
import play.api.libs.json._

import scala.collection.JavaConverters._

import com.github.mustachejava.DefaultMustacheFactory

object MustacheSrv {
  def buildMeetupEventDescription(template: String, meetupOpt: Option[Meetup], venueOpt: Option[Venue], talks: List[Talk], speakers: List[Person]): String = {
    build(template, Map(
      "meetup" -> Json.toJson(meetupOpt.map(m => PublicMeetup.from(m, None, None, None))),
      "venue" -> Json.toJson(venueOpt.map(v => PublicVenue.from(v, None, None, None))),
      "talks" -> Json.toJson(talks.map(t => PublicTalk.from(t, Some(speakers), None, None)))
    ))
  }

  def build(template: String, scopes: Map[String, JsValue]): String = {
    val writer = new StringWriter()
    val mustacheFactory = new DefaultMustacheFactory()
    val mustache = mustacheFactory.compile(new StringReader(template), "dummyFileName")
    mustache.execute(writer, scopes.filter(_._2 != JsNull).map { case (key, value) => (key, format(value)) }.asJava)
    writer.toString.trim
  }

  private def format(json: JsValue): Object = {
    (json match {
      case JsObject(obj) => obj.map { case (key, value) => (key, format(value)) }.asJava
      case JsArray(arr) => arr.map(format).asJava
      case JsString(str) => str
      case JsNumber(n) => n
      case JsBoolean(b) => b
      case JsNull => null
    }).asInstanceOf[Object]
  }
}
