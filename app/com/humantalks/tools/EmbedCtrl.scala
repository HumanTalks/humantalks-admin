package com.humantalks.tools

import com.humantalks.common.services.{ EmbedData, EmbedSrv }
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.{ Results, Action, Controller }

case class EmbedCtrl(ctx: Contexts, embedSrv: EmbedSrv) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def embed(url: String) = Action.async { implicit req =>
    ApiHelper.result(embedSrv.embedRemote(url, Right(EmbedData.unknown(url))), Results.Ok, Results.NotFound)
  }
}
