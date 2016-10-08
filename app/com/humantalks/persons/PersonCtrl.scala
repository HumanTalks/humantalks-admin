package com.humantalks.persons

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.talks.TalkRepository
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class PersonCtrl(ctx: Contexts, talkRepository: TalkRepository, personRepository: PersonRepository)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  val personForm = Form(Person.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    personRepository.find().map { personList =>
      Ok(views.html.list(personList))
    }
  }

  def create = Action.async { implicit req: Request[AnyContent] =>
    formView(Ok, personForm, None)
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => formView(BadRequest, formWithErrors, None),
      personData => personRepository.create(personData, User.fake).map {
        case (_, id) => Redirect(routes.PersonCtrl.get(id))
      }
    )
  }

  def get(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(personRepository)(id) { person =>
      for {
        talkList <- talkRepository.findFor(id)
      } yield Ok(views.html.detail(person, talkList))
    }
  }

  def update(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(personRepository)(id) { person =>
      formView(Ok, personForm.fill(person.data), Some(person))
    }
  }

  def doUpdate(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(personRepository)(id) { person => formView(BadRequest, formWithErrors, Some(person)) },
      personData => CtrlHelper.withItem(personRepository)(id) { person =>
        personRepository.update(person, personData, User.fake).map {
          case _ => Redirect(routes.PersonCtrl.get(id))
        }
      }
    )
  }

  private def formView(status: Status, personForm: Form[Person.Data], personOpt: Option[Person]): Future[Result] = {
    Future(status(views.html.form(personForm, personOpt)))
  }
}
