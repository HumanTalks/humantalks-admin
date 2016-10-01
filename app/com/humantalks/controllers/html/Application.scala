package com.humantalks.controllers.html

import global.Contexts
import play.api.mvc.{ Controller, Action, Request, AnyContent }

case class Application(ctx: Contexts) extends Controller {
  import ctx._
  import Contexts.ctrlToEC

  def index = Action { implicit req: Request[AnyContent] =>
    Ok(com.humantalks.views.html.index())
  }
}
