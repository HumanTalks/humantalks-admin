package global.controllers

import global.Contexts
import global.helpers.ApiHelper
import global.infrastructure.Mongo
import play.api.i18n.{ Lang, MessagesApi }
import play.api.libs.json.Json
import play.api.mvc.{ Action, Controller, Results }

case class Application(ctx: Contexts, db: Mongo)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def status = Action.async { implicit req =>
    ApiHelper.resultJson({
      for {
        dbStatus <- db.pingStatus()
      } yield {
        Right(Json.obj(
          "build" -> Json.obj(
            "date" -> global.BuildInfo.builtAtString,
            "timestamp" -> global.BuildInfo.builtAtMillis,
            "commit" -> global.BuildInfo.gitHash,
            "version" -> global.BuildInfo.version
          ),
          "checks" -> List(Json.obj(
            "name" -> "database",
            "test" -> "ping",
            "status" -> dbStatus.code,
            "message" -> dbStatus.message
          ))
        ))
      }
    }, Results.Ok, Results.InternalServerError)
  }

  def changeLang(lang: String) = Action { implicit req =>
    messageApi.setLang(Redirect(req.headers.get("Referer").orElse(req.headers.get("Host")).get), new Lang(lang))
  }
}
