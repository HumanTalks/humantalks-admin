package com.humantalks.talks

import com.humantalks.common.models.User
import com.humantalks.persons.PersonRepository
import com.humantalks.talks.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class TalkCtrl(ctx: Contexts, talkRepository: TalkRepository, personRepository: PersonRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val talkForm = Form(Talk.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    for {
      talkList <- talkRepository.find()
      personMap <- personRepository.findByIds(talkList.flatMap(_.data.speakers)).map(_.map(p => (p.id, p)).toMap)
    } yield Ok(html.list(talkList, personMap))
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, talkForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      talkData => {
        talkRepository.create(talkData, User.fake).map {
          case (success, id) => Redirect(routes.TalkCtrl.get(id))
        }
      }
    )
  }

  def get(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    withTalk(id) { talk =>
      personRepository.findByIds(talk.data.speakers).map { personList =>
        Ok(html.detail(talk, personList.map(p => (p.id, p)).toMap))
      }
    }
  }

  def update(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    withTalk(id) { talk =>
      formView(Ok, talkForm.fill(talk.data), Some(talk))
    }
  }

  def doUpdate(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => withTalk(id) { talk => formView(BadRequest, formWithErrors, Some(talk)) },
      talkData => {
        talkRepository.get(id).flatMap { talkOpt =>
          talkOpt.map { talk =>
            talkRepository.update(talk.copy(data = talkData), User.fake).map {
              case success => Redirect(routes.TalkCtrl.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def formView(status: Status, talkForm: Form[Talk.Data], talkOpt: Option[Talk]): Future[Result] = {
    personRepository.find().map { personList =>
      status(html.form(talkForm, talkOpt, personList))
    }
  }
  private def notFound(id: Talk.Id): Result =
    NotFound(com.humantalks.common.views.html.errors.notFound("Unable to find a Talk with id " + id))
  private def withTalk(id: Talk.Id)(block: Talk => Future[Result]): Future[Result] = {
    talkRepository.get(id).flatMap { talkOpt =>
      talkOpt.map { talk =>
        block(talk)
      }.getOrElse {
        Future(notFound(id))
      }
    }
  }
}
