package com.humantalks.exposed

import global.Contexts
import play.api.mvc.{ Action, Controller }

case class Application(ctx: Contexts) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = Action { implicit req =>
    Ok(views.html.index())
  }
}
