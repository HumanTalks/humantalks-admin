package com.humantalks.internal

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.internal.events.EventDbService
import com.humantalks.internal.partners.PartnerDbService
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.TalkDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import org.joda.time.LocalDate
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

case class Application(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    personDbService: PersonDbService,
    partnerDbService: PartnerDbService,
    talkDbService: TalkDbService,
    eventDbService: EventDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = silhouette.SecuredAction.async { implicit req =>
    implicit val user = Some(req.identity)
    if (req.identity.isAuthorized(Person.Role.Organizer)) {
      for {
        nextEvent <- eventDbService.getNext
        partnerList <- nextEvent.map(e => partnerDbService.findByIds(e.allPartners)).getOrElse(Future.successful(List()))
        talkList <- nextEvent.map(e => talkDbService.findByIds(e.data.talks)).getOrElse(Future.successful(List()))
        personList <- personDbService.findByIds(talkList.flatMap(_.data.speakers))
        sponsors <- partnerDbService.findSponsors()
      } yield {
        val (currentSponsors, oldSponsors) = sponsors
          .sortBy(_.lastSponsor().map(_.end.toDate.getTime).getOrElse(0l))
          .partition(_.isSponsor(new LocalDate()))
        Ok(views.html.index(nextEvent, partnerList, talkList, personList, currentSponsors, oldSponsors))
      }
    } else {
      Future.successful(Ok(views.html.index(None, List(), List(), List(), List(), List())))
    }
  }

  def apiRoot = silhouette.SecuredAction.async { implicit req =>
    ApiHelper.resultJson(Future.successful(Right(Json.obj("api" -> "internalApi"))), Results.Ok, Results.InternalServerError)
  }
}
