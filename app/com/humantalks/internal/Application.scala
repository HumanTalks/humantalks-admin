package com.humantalks.internal

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.Future

case class Application(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv]
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def index = silhouette.SecuredAction { implicit req =>
    implicit val user = Some(req.identity)
    Ok(views.html.index())
  }

  def apiRoot = silhouette.SecuredAction.async { implicit req =>
    ApiHelper.resultJson(Future(Right(Json.obj("api" -> "internalApi"))), Results.Ok, Results.InternalServerError)
  }
}
