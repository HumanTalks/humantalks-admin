package com.humantalks.talks

import com.humantalks.auth.models.User
import com.humantalks.meetups.MeetupRepository
import com.humantalks.persons.{ Person, PersonRepository }
import global.Contexts
import global.helpers.CtrlHelper
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class TalkCtrl(ctx: Contexts, meetupRepository: MeetupRepository, personRepository: PersonRepository, talkDbService: TalkDbService)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val talkForm = Form(Talk.fields)
  val personForm = Form(Person.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      talkList <- talkDbService.find()
      personList <- personRepository.findByIds(talkList.flatMap(_.data.speakers))
    } yield Ok(views.html.list(talkList, personList))
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, talkForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      talkData => talkDbService.create(talkData, User.fake).map {
        case (_, id) => Redirect(routes.TalkCtrl.get(id))
      }
    )
  }

  def get(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      for {
        personList <- personRepository.findByIds(talk.data.speakers)
        meetupList <- meetupRepository.findForTalk(id)
      } yield Ok(views.html.detail(talk, personList, meetupList))
    }
  }

  def update(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(talkDbService)(id) { talk =>
      formView(Ok, talkForm.fill(talk.data), Some(talk))
    }
  }

  def doUpdate(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(talkDbService)(id) { talk => formView(BadRequest, formWithErrors, Some(talk)) },
      talkData => CtrlHelper.withItem(talkDbService)(id) { talk =>
        talkDbService.update(talk, talkData, User.fake).map {
          case _ => Redirect(routes.TalkCtrl.get(id))
        }
      }
    )
  }

  private def formView(status: Status, talkForm: Form[Talk.Data], talkOpt: Option[Talk]): Future[Result] = {
    personRepository.find().map { personList =>
      status(views.html.form(talkForm, talkOpt, personList, personForm))
    }
  }
}
