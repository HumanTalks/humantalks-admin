package com.humantalks.exposed

import com.humantalks.internal.persons.{ PersonDbService, Person }
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.{ Results, Action, Controller }

import scala.concurrent.Future

case class Application(
    ctx: Contexts,
    personDbService: PersonDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = Action { implicit req =>
    Ok(views.html.index())
  }

  def apiRoot = Action.async { implicit req =>
    ApiHelper.resultJson(Future(Right(Json.obj("api" -> "exposedApi"))), Results.Ok, Results.InternalServerError)
  }

  def createPerson = Action.async(parse.json) { implicit req => ApiHelper.create(personDbService, Person.anonymous, req.body) }
}
