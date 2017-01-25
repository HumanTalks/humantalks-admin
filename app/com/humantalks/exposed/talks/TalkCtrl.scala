package com.humantalks.exposed.talks

import com.humantalks.common.Conf
import com.humantalks.common.services.{ NotificationSrv, DateSrv }
import com.humantalks.internal.persons.{ Person, PersonDbService }
import com.humantalks.internal.talks.{ Talk, TalkDbService }
import global.Contexts
import global.helpers.CtrlHelper
import org.joda.time.{ DateTimeConstants, DateTime }
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc.{ Action, Controller, Result }

import scala.concurrent.Future

case class TalkCtrl(
    conf: Conf,
    ctx: Contexts,
    personDbService: PersonDbService,
    talkDbService: TalkDbService,
    notificationSrv: NotificationSrv
)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.wsToEC
  import ctx._
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def create() = Action.async { implicit req =>
    formView(Ok, talkForm, None)
  }

  def doCreate() = Action.async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      data => for {
        (_, id) <- talkDbService.create(data, data.speakers.head)
        talk <- talkDbService.get(id).map(_.get)
        speakers <- personDbService.findByIds(talk.data.speakers)
        success <- notificationSrv.proposalCreated(talk, speakers)
      } yield Ok(views.html.submitted(talk.id, data))
    )
  }

  def update(id: Talk.Id) = Action.async { implicit req =>
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      formView(Ok, talkForm.fill(talk.data), Some(talk))
    }
  }

  def doUpdate(id: Talk.Id) = Action.async { implicit req =>
    talkForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(talkDbService)(id) { talk => formView(BadRequest, formWithErrors, Some(talk)) },
      data => CtrlHelper.withItem(talkDbService)(id) { talk =>
        talkDbService.update(talk, data, data.speakers.head).map { _ =>
          Ok(views.html.submitted(id, data))
        }
      }
    )
  }

  private def formView(status: Status, talkForm: Form[Talk.Data], talkOpt: Option[Talk]): Future[Result] = {
    personDbService.find().map { personList =>
      val now = new DateTime()
      val nextDates = (1 to 7).map(i => now.plusMonths(i)).map(d => DateSrv.getNthDayOfMonth(d, 2, DateTimeConstants.TUESDAY)).filter(_.toDateTimeAtCurrentTime.isAfterNow).toList
      status(views.html.form(talkForm, talkOpt, personList, nextDates, personForm))
    }
  }
}
