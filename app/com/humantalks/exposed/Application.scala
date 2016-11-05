package com.humantalks.exposed

import controllers.Assets
import global.Contexts
import play.api.mvc.{ Action, Controller }

case class Application(
    ctx: Contexts
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = Action.async { implicit req =>
    Assets.at("/public", "index.html").apply(req)
  }
}
