package com.humantalks.internal.common.controllers

import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

case class Application(ctx: Contexts) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = Action { implicit req: Request[AnyContent] =>
    Ok(com.humantalks.internal.common.views.html.index())
  }

  def apiRoot = Action.async { implicit req: Request[AnyContent] =>
    ApiHelper.resultJson(Future(Right(Json.obj("api" -> "internalApi"))), Results.Ok, Results.InternalServerError)
  }
}
