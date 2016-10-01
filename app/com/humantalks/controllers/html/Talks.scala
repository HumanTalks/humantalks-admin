package com.humantalks.controllers.html

import com.humantalks.domain.{ Talk, User, TalkData }
import com.humantalks.infrastructure.{ PersonRepository, TalkRepository }
import com.humantalks.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class Talks(ctx: Contexts, talkRepository: TalkRepository, personRepository: PersonRepository)(implicit messageApi: MessagesApi) extends Controller {
  import ctx._
  import Contexts.ctrlToEC
  val talkForm = Form(TalkData.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    talkRepository.find().map { talkList =>
      Ok(html.talks.list(talkList))
    }
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, talkForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    talkForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      talkData => {
        talkRepository.create(talkData, User.fake).map {
          case (success, id) => Redirect(routes.Talks.get(id))
        }
      }
    )
  }

  def get(id: Talk.Id) = Action.async { implicit req: Request[AnyContent] =>
    withTalk(id) { talk =>
      Future(Ok(html.talks.detail(talk)))
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
              case success => Redirect(routes.Talks.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def formView(status: Status, talkForm: Form[TalkData], talkOpt: Option[Talk]): Future[Result] = {
    personRepository.find().map { personList =>
      status(html.talks.form(talkForm, talkOpt, personList))
    }
  }
  private def notFound(id: Talk.Id): Result =
    NotFound(html.errors.notFound("Unable to find a Talk with id " + id))
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
