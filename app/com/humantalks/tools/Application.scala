package com.humantalks.tools

import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc.{ Results, Action, Controller }

import scala.concurrent.Future

case class Application(ctx: Contexts) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def apiRoot = Action.async { implicit req =>
    ApiHelper.resultJson(Future(Right(Json.obj("api" -> "toolApi"))), Results.Ok, Results.InternalServerError)
  }
}
