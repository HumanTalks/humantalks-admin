package com.humantalks.common.controllers

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.meetups.MeetupDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.TalkDbService
import com.humantalks.internal.venues.VenueDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

case class Select2Option(id: String, text: String)

case class Select2Ctrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._
  implicit val format = Json.format[Select2Option]

  def venues = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      venueDbService.find().map { venues =>
        Right(venues.map(v => Select2Option(v.id.value, v.data.name + v.data.location.map(l => " (" + l.formatted + ")").getOrElse(""))))
      }
    })
  }

  def persons = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      personDbService.find().map { persons =>
        Right(persons.map(p => Select2Option(p.id.value, p.data.name)))
      }
    })
  }

  def talks = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      talkDbService.find().flatMap { talks =>
        personDbService.findByIds(talks.flatMap(_.data.speakers)).map { speakers =>
          Right(talks.map(t => Select2Option(
            t.id.value,
            t.data.title + t.data.speakers.flatMap(id => speakers.find(_.id == id)).map(_.data.name).mkString(" (", ", ", ")")
          )))
        }
      }
    })
  }

  def meetups = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      meetupDbService.find().map { meetups =>
        Right(meetups.map(m => Select2Option(m.id.value, m.data.title)))
      }
    })
  }
}
