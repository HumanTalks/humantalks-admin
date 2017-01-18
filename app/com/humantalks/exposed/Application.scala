package com.humantalks.exposed

import global.Contexts
import play.api.mvc.{ Action, Controller }

case class Application(
    ctx: Contexts
) extends Controller {
  def index = Action { implicit req =>
    Ok(views.html.index())
  }
}
