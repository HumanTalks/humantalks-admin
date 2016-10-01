package com.humantalks.persons

import com.humantalks.common.models.User
import com.humantalks.persons.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.Future

case class PersonCtrl(ctx: Contexts, personRepository: PersonRepository)(implicit messageApi: MessagesApi) extends Controller {
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
      personData => {
        personRepository.create(personData, User.fake).map {
          case (success, id) => Redirect(routes.PersonCtrl.get(id))
        }
      }
    )
  }

  def get(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    withPerson(id) { person =>
      Ok(html.detail(person))
    }
  }

  def update(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    withPerson(id) { person =>
      Ok(html.form(personForm.fill(person.data), Some(person)))
    }
  }

  def doUpdate(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => withPerson(id) { person => BadRequest(html.form(formWithErrors, Some(person))) },
      personData => {
        personRepository.get(id).flatMap { personOpt =>
          personOpt.map { person =>
            personRepository.update(person.copy(data = personData), User.fake).map {
              case success => Redirect(routes.PersonCtrl.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def notFound(id: Person.Id): Result =
    NotFound(com.humantalks.common.views.html.errors.notFound("Unable to find a Person with id " + id))
  private def withPerson(id: Person.Id)(block: Person => Result): Future[Result] = {
    personRepository.get(id).map { personOpt =>
      personOpt.map { person =>
        block(person)
      }.getOrElse {
        notFound(id)
      }
    }
  }
}
