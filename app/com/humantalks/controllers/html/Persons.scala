package com.humantalks.controllers.html

import com.humantalks.domain.{ User, PersonData, Person }
import com.humantalks.infrastructure.PersonRepository
import com.humantalks.views.html
import global.Contexts
import play.api.data.Form
import play.api.i18n.MessagesApi
import play.api.mvc._
import scala.concurrent.Future

case class Persons(ctx: Contexts, personRepository: PersonRepository)(implicit messageApi: MessagesApi) extends Controller {
  import ctx._
  import Contexts.ctrlToEC
  val personForm = Form(PersonData.fields)

  def find = Action.async { implicit req: Request[AnyContent] =>
    personRepository.find().map { personList =>
      Ok(html.persons.list(personList))
    }
  }

  def create = Action { implicit req: Request[AnyContent] =>
    Ok(html.persons.form(personForm, None))
  }

  def doCreate() = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => Future(BadRequest(html.persons.form(formWithErrors, None))),
      personData => {
        personRepository.create(personData, User.fake).map {
          case (success, id) => Redirect(routes.Persons.get(id))
        }
      }
    )
  }

  def get(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    withPerson(id) { person =>
      Ok(html.persons.detail(person))
    }
  }

  def update(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    withPerson(id) { person =>
      Ok(html.persons.form(personForm.fill(person.data), Some(person)))
    }
  }

  def doUpdate(id: Person.Id) = Action.async { implicit req: Request[AnyContent] =>
    personForm.bindFromRequest.fold(
      formWithErrors => withPerson(id) { person => BadRequest(html.persons.form(formWithErrors, Some(person))) },
      personData => {
        personRepository.get(id).flatMap { personOpt =>
          personOpt.map { person =>
            personRepository.update(person.copy(data = personData), User.fake).map {
              case success => Redirect(routes.Persons.get(id))
            }
          }.getOrElse {
            Future(notFound(id))
          }
        }
      }
    )
  }

  private def notFound(id: Person.Id): Result =
    NotFound(html.errors.notFound("Unable to find a Person with id " + id))
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
