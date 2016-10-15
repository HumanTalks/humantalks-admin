package com.humantalks.tools.scrapers

import com.humantalks.common.services.TwitterSrv
import global.Contexts
import global.helpers.ScraperHelper
import play.api.libs.ws.WSClient
import play.api.mvc.{ Action, Controller }

case class TwitterScraper(ctx: Contexts, ws: WSClient) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  private val baseUrl = "https://twitter.com"
  private def accountUrl(account: String): String = baseUrl + "/" + TwitterSrv.toAccount(account)

  def profil(account: String) = Action.async { implicit req =>
    ScraperHelper.scrapeHtml(ws)(accountUrl(account))(TwitterSrv.htmlToAccount)
  }
}
