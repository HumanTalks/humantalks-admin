package com.humantalks.tools.scrapers

import global.Contexts
import global.helpers.{ ApiHelper, StringHelper }
import play.api.libs.json.{ JsValue, JsObject, JsArray, Json }
import play.api.libs.ws.WSClient
import play.api.mvc.{ Action, Controller }

case class EmailScraper(ctx: Contexts, ws: WSClient) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  private val gravatarUrl = "https://www.gravatar.com"
  private def gravatarProfile(email: String): String = gravatarUrl + "/" + StringHelper.toMd5(email.trim.toLowerCase) + ".json"

  def profil(email: String) = Action.async { implicit req =>
    ApiHelper.resultJson({
      ws.url(gravatarProfile(email)).get().map { res =>
        val json = (res.json \ "entry" \ 0).as[JsValue]
        Right(Json.obj(
          "name" -> (json \ "displayName").asOpt[String],
          "username" -> (json \ "preferredUsername").asOpt[String],
          "avatar" -> (json \ "thumbnailUrl").asOpt[String],
          "backgroundImage" -> (json \ "profileBackground" \ "url").asOpt[String],
          "bio" -> (json \ "aboutMe").asOpt[String],
          "site" -> (json \ "urls" \ 0 \ "value").asOpt[String],
          "twitter" -> findAccounts(json).flatMap(findAccount("twitter")).flatMap(accountToUsername),
          "linkedin" -> findAccounts(json).flatMap(findAccount("linkedin")).flatMap(accountToUsername),
          "accounts" -> findAccounts(json)
        ) ++ res.json.as[JsObject])
      }.recover {
        case e: Throwable => Right(Json.obj(
          "name" -> "",
          "username" -> "",
          "avatar" -> "",
          "backgroundImage" -> "",
          "bio" -> "",
          "site" -> "",
          "twitter" -> "",
          "linkedin" -> "",
          "error" -> e.getMessage
        ))
      }
    })
  }
  private def findAccounts(json: JsValue): Option[List[JsObject]] =
    (json \ "accounts").asOpt[List[JsObject]]
  private def findAccount(name: String)(accounts: List[JsObject]): Option[JsObject] =
    accounts.find(json => (json \ "shortname").asOpt[String].contains(name))
  private def accountToUsername(account: JsObject): Option[String] =
    (account \ "username").asOpt[String]
}
