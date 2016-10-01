package global.controllers

import global.Contexts
import global.helpers.ApiHelper
import global.infrastructure.Mongo
import org.joda.time.DateTime
import play.api.i18n.{ Lang, MessagesApi }
import play.api.libs.json.Json
import play.api.mvc.{ Action, AnyContent, Controller, Request, Results }

case class Application(ctx: Contexts, db: Mongo)(implicit messageApi: MessagesApi) extends Controller {
  import Contexts.ctrlToEC
  import ctx._

  def status = Action.async { implicit req: Request[AnyContent] =>
    val start = new DateTime()
    for {
      dbStatus <- db.pingStatus()
    } yield {
      ApiHelper.writeResult(Results.Ok, Json.obj(
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
        )),
        "metas" -> ApiHelper.metas(start)
      ))
    }
  }

  def changeLang(lang: String) = Action { implicit req: Request[AnyContent] =>
    messageApi.setLang(Redirect(req.headers.get("Referer").orElse(req.headers.get("Host")).get), new Lang(lang))
  }
}
