package com.humantalks.common.controllers

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.events.EventDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.{ Talk, TalkDbService }
import com.humantalks.internal.partners.PartnerDbService
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
    partnerDbService: PartnerDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    eventDbService: EventDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._
  implicit val format = Json.format[Select2Option]

  def partners = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      partnerDbService.find().map { partners =>
        val options = partners
          .filter(_.data.venue.isDefined)
          .map(v => Select2Option(v.id.value, v.data.name + v.data.venue.map(v => " (" + v.location.formatted + ")").getOrElse("")))
        Right(options)
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

  def talks(pending: Boolean) = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      (if (pending) talkDbService.findPending() else talkDbService.find()).flatMap { talks =>
        personDbService.findByIds(talks.flatMap(_.data.speakers)).map { personList =>
          Right(talks.map(t => {
            val speakers = t.data.speakers.flatMap(id => personList.find(_.id == id))
            Select2Option(
              t.id.value,
              t.data.title + (if (speakers.nonEmpty) speakers.map(_.data.name).mkString(" (", ", ", ")") else "")
            )
          }))
        }
      }
    })
  }

  def events = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async { implicit req =>
    ApiHelper.resultList({
      eventDbService.find().map { events =>
        Right(events.map(m => Select2Option(m.id.value, m.data.title)))
      }
    })
  }
}
