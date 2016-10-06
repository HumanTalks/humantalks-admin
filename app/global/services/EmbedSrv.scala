package global.services

import play.api.libs.json.Json

import scala.concurrent.Future

case class EmbedData(
  originUrl: String,
  service: String,
  embedUrl: String,
  embedCode: String
)
object EmbedData {
  implicit val format = Json.format[EmbedData]
}

object EmbedSrv {
  def embed(url: String): Option[EmbedData] = url match {
    case youtube1(videoId) => Some(youtubeEmbedCode(url, videoId))
    case youtube2(videoId) => Some(youtubeEmbedCode(url, videoId))
    case dailymotion(videoId) => Some(dailymotionEmbedCode(url, videoId))
    case vimeo(videoId) => Some(vimeoEmbedCode(url, videoId))
    case googleslides(slidesId) => Some(googleslidesEmbedCode(url, slidesId))
    case slidescom(user, slidesId) => Some(slidescomEmbedCode(url, user, slidesId))
    case pdf() => Some(pdfEmbedCode(url))
    case _ => None
  }
  def resolveAndEmbed(url: String): Future[Option[EmbedData]] = ???

  private val hash = "([^/?&#]+)"
  val youtube1 = "https?://www.youtube.com/watch\\?(?:[^=]+=[^&]+&)*v=([^&]+).*".r
  val youtube2 = s"https?://youtu.be/$hash.*".r
  val dailymotion = s"https?://www.dailymotion.com/video/$hash.*".r
  val vimeo = s"https?://vimeo.com/$hash.*".r
  //val infoq = s"https?://www.infoq.com/presentations/$hash".r

  val googleslides = s"https?://docs.google.com/presentation/d/$hash.*".r
  val slidescom = s"https?://slides.com/$hash/$hash.*".r
  val pdf = s".*\\.pdf".r
  //val slideshare = s"https?://[a-z]+.slideshare.net/$hash/$hash.*".r
  //val speakerdeck = s"https?://speakerdeck.com/$hash/$hash.*".r
  //val prezi = s"https?://prezi.com/p/$hash.*".r

  private def youtubeEmbedCode(url: String, videoId: String): EmbedData = {
    val embedUrl = s"https://www.youtube.com/embed/$videoId"
    EmbedData(
      url,
      "youtube",
      embedUrl,
      s"""<iframe width="560" height="315" src="$embedUrl" frameborder="0" allowfullscreen></iframe>"""
    )
  }
  private def dailymotionEmbedCode(url: String, videoId: String): EmbedData = {
    val videoHash = videoId.split("_").headOption.getOrElse(videoId)
    val embedUrl = s"//www.dailymotion.com/embed/video/$videoHash"
    EmbedData(
      url,
      "dailymotion",
      embedUrl,
      s"""<iframe src="$embedUrl" width="560" height="315" frameborder="0" allowfullscreen></iframe>"""
    )
  }
  private def vimeoEmbedCode(url: String, videoId: String): EmbedData = {
    val embedUrl = s"https://player.vimeo.com/video/$videoId"
    EmbedData(
      url,
      "vimeo",
      embedUrl,
      s"""<iframe src="$embedUrl" width="640" height="360" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>"""
    )
  }
  private def googleslidesEmbedCode(url: String, slidesId: String): EmbedData = {
    val embedUrl = s"https://docs.google.com/presentation/d/$slidesId/embed"
    EmbedData(
      url,
      "googleslides",
      embedUrl,
      s"""<iframe src="$embedUrl" width="960" height="569" frameborder="0" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true"></iframe>"""
    )
  }
  private def slidescomEmbedCode(url: String, user: String, slidesId: String): EmbedData = {
    val embedUrl = s"//slides.com/$user/$slidesId/embed?style=light"
    EmbedData(
      url,
      "slidescom",
      embedUrl,
      s"""<iframe src="$embedUrl" width="576" height="420" scrolling="no" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>"""
    )
  }
  /*private def slideshareEmbedCode(url: String): Future[Option[EmbedData]] = {
    WS.url(url).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val embedUrl = doc.select("meta.twitter_player").attr("value")
      if(embedUrl != null && embedUrl.length > 0) {
        Some(EmbedData(
          url,
          "slideshare",
          embedUrl,
          s"""<iframe src="$embedUrl" width="595" height="485" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" style="border:1px solid #CCC; border-width:1px; margin-bottom:5px; max-width: 100%;" allowfullscreen></iframe>"""))
      } else {
        None
      }
    }
  }
  private def speakerdeckEmbedCode(url: String): Future[Option[EmbedData]] = {
    WS.url(url).get().map { response =>
      val doc = Jsoup.parse(response.body)
      val embedElt = doc.select("div.speakerdeck-embed")
      val embedId = embedElt.attr("data-id")
      val embedRatio = embedElt.attr("data-ratio")
      if(embedId != null && embedId.length > 0) {
        Some(EmbedData(
          url,
          "speakerdeck",
          url,
          s"""<script async class="speakerdeck-embed" data-id="$embedId" data-ratio="$embedRatio" src="//speakerdeck.com/assets/embed.js"></script>"""))
      } else {
        None
      }
    }
  }*/
  private def pdfEmbedCode(url: String): EmbedData = {
    EmbedData(
      url,
      "pdf",
      url,
      s"""<object data="$url" type="application/pdf" width="640" height="480">alt : <a href="$url">$url</a></object>"""
    )
  }
}
