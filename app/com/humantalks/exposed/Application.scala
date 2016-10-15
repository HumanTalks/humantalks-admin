package com.humantalks.exposed

import global.Contexts
import play.api.mvc.{ Action, AnyContent, Request, Controller }

case class Application(ctx: Contexts) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = Action { implicit req: Request[AnyContent] =>
    Ok(views.html.index())
  }
}
