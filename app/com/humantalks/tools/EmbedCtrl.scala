package com.humantalks.tools

import com.humantalks.common.services.{ EmbedData, EmbedSrv }
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.{ Results, Action, Controller }

case class EmbedCtrl(ctx: Contexts, embedSrv: EmbedSrv) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def embed(url: String) = Action.async { implicit req =>
    ApiHelper.result(embedSrv.embedRemote(url).map { res =>
      req.queryString.get("debug").flatMap(_.find(_ == "true")).map { _ =>
        res
      }.getOrElse {
        res.left.flatMap(_ => Right(EmbedData.unknown(url)))
      }
    }, Results.Ok, Results.NotFound)
  }
}
