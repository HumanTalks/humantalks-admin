package com.humantalks.tools.scrapers

import com.humantalks.common.helpers.ScraperHelper
import com.humantalks.common.services.TwitterSrv
import global.Contexts
import play.api.libs.ws.WSClient
import play.api.mvc.{ Action, Controller, AnyContent, Request }

case class TwitterScraper(ctx: Contexts, ws: WSClient) extends Controller {
  import Contexts.ctrlToEC
  import ctx._
  private val baseUrl = "https://twitter.com"
  private def accountUrl(account: String): String = baseUrl + "/" + TwitterSrv.toAccount(account)

  def profil(account: String) = Action.async { implicit req: Request[AnyContent] =>
    ScraperHelper.scrapeHtml(ws)(accountUrl(account))(TwitterSrv.htmlToAccount)
  }
}
