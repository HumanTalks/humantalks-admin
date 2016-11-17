package com.humantalks.internal.admin.config

import com.humantalks.auth.authorizations.WithRole
import com.humantalks.auth.silhouette.SilhouetteEnv
import com.humantalks.common.services.MustacheSrv
import com.humantalks.internal.meetups.{ Meetup, MeetupDbService }
import com.humantalks.internal.persons.{ PersonDbService, Person }
import com.humantalks.internal.talks.TalkDbService
import com.humantalks.internal.venues.VenueDbService
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.Future

case class ConfigApiCtrl(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    venueDbService: VenueDbService,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    meetupDbService: MeetupDbService,
    configDbService: ConfigDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def preview = silhouette.SecuredAction(WithRole(Person.Role.Organizer)).async(parse.json) { implicit req =>
    (for {
      ref <- (req.body \ "ref").asOpt[String]
      value <- (req.body \ "value").asOpt[String]
    } yield ref match {
      case Config.meetupEventDescription.ref => for {
        meetupOpt <- (req.body \ "id").asOpt[String].flatMap(id => Meetup.Id.from(id).right.toOption).map(id => meetupDbService.get(id)).getOrElse(meetupDbService.getNext)
        venueOpt <- meetupOpt.flatMap(_.data.venue).map(venueDbService.get).getOrElse(Future.successful(None))
        talks <- meetupOpt.map(meetup => talkDbService.findByIds(meetup.data.talks)).getOrElse(Future.successful(List()))
        speakers <- personDbService.findByIds(talks.flatMap(_.data.speakers))
      } yield Ok(MustacheSrv.buildMeetupEventDescription(value, meetupOpt, venueOpt, talks, speakers))
      case _ => Future.successful(NotFound(s"No preview defined for ref '$ref'"))
    }).getOrElse(Future.successful(BadRequest(s"Incorrect payload format :\n\n${Json.prettyPrint(req.body)}\n\nAttributes 'ref' & 'value' are required !")))
  }
}
