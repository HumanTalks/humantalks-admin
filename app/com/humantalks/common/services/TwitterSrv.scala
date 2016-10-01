package com.humantalks.common.services

import com.humantalks.common.helpers.ScraperHelper
import org.jsoup.Jsoup
import play.api.libs.json.Json

object TwitterSrv {
  def toAccount(twitterAccount: String): String = twitterAccount.trim.replace("@", "").replaceAll("https?://twitter.com/", "")
  def toHashtag(twitterHashtag: String): String = twitterHashtag.trim.replace("#", "").replaceAll("https?://twitter.com/hashtag/", "").replaceAll("https?://twitter.com/search?q=%23", "").replace("?src=hash", "").replace("&src=typd", "")

  case class Account(
    name: String,
    account: String,
    avatar: String,
    backgroundImage: String,
    bio: String,
    location: String,
    url: String,
    tweets: Int,
    following: Int,
    followers: Int,
    favorites: Int
  )

  def htmlToAccount(html: String, url: String): Account = {
    val doc = Jsoup.parse(html)
    Account(
      name = doc.select(".ProfileHeaderCard-name a").text(),
      account = doc.select(".ProfileHeaderCard-screenname a span").text(),
      avatar = doc.select(".ProfileAvatar-image").attr("src"),
      backgroundImage = doc.select(".ProfileCanopy-headerBg img").attr("src"),
      bio = doc.select(".ProfileHeaderCard-bio").text(),
      location = doc.select(".ProfileHeaderCard-locationText").text(),
      url = doc.select(".ProfileHeaderCard-urlText a").attr("title"),
      tweets = ScraperHelper.toInt(doc.select(".ProfileNav-item--tweets a").attr("title")).getOrElse(-1),
      following = ScraperHelper.toInt(doc.select(".ProfileNav-item--following a").attr("title")).getOrElse(-1),
      followers = ScraperHelper.toInt(doc.select(".ProfileNav-item--followers a").attr("title")).getOrElse(-1),
      favorites = ScraperHelper.toInt(doc.select(".ProfileNav-item--favorites a").attr("title")).getOrElse(-1)
    )
  }

  implicit val formatAccount = Json.format[Account]
}
