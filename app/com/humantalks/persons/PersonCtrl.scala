package com.humantalks.persons

import com.humantalks.common.helpers.CtrlHelper
import com.humantalks.common.models.User
import com.humantalks.persons.views.html
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
      Ok(html.list(personList))
    }
  }

  def create = Action { implicit req: Request[AnyContent] =>
    Ok(html.form(personForm, None))
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(html.form(formWithErrors, None))),
      personData => personRepository.create(personData, User.fake).map {
        case (success, id) => Redirect(routes.PersonCtrl.get(id))
      }
    )
  }

  def get(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(personRepository)(id) { person =>
      for {
        talkList <- talkRepository.findFor(id)
      } yield Ok(html.detail(person, talkList))
    }
  }

  def update(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    CtrlHelper.withItem(personRepository)(id) { person =>
      Future(Ok(html.form(personForm.fill(person.data), Some(person))))
    }
  }

  def doUpdate(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => CtrlHelper.withItem(personRepository)(id) { person => Future(BadRequest(html.form(formWithErrors, Some(person)))) },
      personData => CtrlHelper.withItem(personRepository)(id) { person =>
        personRepository.update(person, personData, User.fake).map {
          case success => Redirect(routes.PersonCtrl.get(id))
        }
      }
    )
  }
}
