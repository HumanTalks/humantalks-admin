package com.humantalks.common.controllers

import global.Contexts
import play.api.mvc.{ Action, AnyContent, Controller, Request }

case class Application(ctx: Contexts) extends Controller {

  def index = Action { implicit req: Request[AnyContent] =>
    Ok(com.humantalks.common.views.html.index())
  }
}
