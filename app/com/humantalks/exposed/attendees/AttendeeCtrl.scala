package com.humantalks.exposed.attendees

import com.humantalks.internal.attendees.AttendeeDbService
import com.humantalks.internal.events.{ Event, EventDbService }
import global.Contexts
import global.helpers.CtrlHelper
import play.api.i18n.MessagesApi
import play.api.mvc.{ Action, Controller }

case class AttendeeCtrl(
    ctx: Contexts,
    eventDbService: EventDbService,
    attendeeDbService: AttendeeDbService
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def checkin(id: Event.Id) = Action.async { implicit req =>
    CtrlHelper.withItem(eventDbService)(id) { event =>
      for {
        attendees <- attendeeDbService.findByEvent(id)
      } yield Ok(views.html.checkin(event, attendees))
    }
  }
}
