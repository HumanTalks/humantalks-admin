package com.humantalks.common.services

import global.models.ApiError
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Future }

case class EmbedData(
  originUrl: String,
  service: String,
  embedUrl: String,
  embedCode: String
)
object EmbedData {
  def unknown(url: String): EmbedData = EmbedData(url, "Unknown", url, s"""<a href="$url" target="_blank" embed-url="$url">$url</a>""")
  implicit val format = Json.format[EmbedData]
}

case class EmbedSrv(ws: WSClient) {
  def embed(url: String, default: Either[ApiError, EmbedData] = EmbedSrv.default): Either[ApiError, EmbedData] = EmbedSrv.embed(url, default)
  def embedRemote(url: String, default: Either[ApiError, EmbedData] = EmbedSrv.default)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedData]] = EmbedSrv.embedRemote(ws)(url, default)
  def resolve(url: String)(implicit ec: ExecutionContext): Future[String] = EmbedSrv.resolve(ws)(url)
  def resolveAndEmbed(url: String, default: Either[ApiError, EmbedData] = EmbedSrv.default)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedData]] = resolve(url).flatMap(u => embedRemote(u, default))
}
object EmbedSrv {
  private val default = Left(ApiError.notFound("Unknown service"))
  def embed(url: String, default: Either[ApiError, EmbedData] = default): Either[ApiError, EmbedData] = url match {
    case YouTube.url1(videoId) => Right(YouTube.embed(url, videoId))
    case YouTube.url2(videoId) => Right(YouTube.embed(url, videoId))
    case Dailymotion.url(videoId) => Right(Dailymotion.embed(url, videoId))
    case Vimeo.url(videoId) => Right(Vimeo.embed(url, videoId))
    case GoogleSlides.url(slidesId) => Right(GoogleSlides.embed(url, slidesId))
    case SlidesDotCom.url(user, slidesId) => Right(SlidesDotCom.embed(url, user, slidesId))
    case Pdf.url() => Right(Pdf.embed(url))
    case _ => default
  }

  def embedRemote(ws: WSClient)(url: String, default: Either[ApiError, EmbedData] = default)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedData]] = url match {
    case SlideShare.url(user, slidesId) => SlideShare.embed(ws)(url)
    case SpeakerDeck.url(user, slidesId) => SpeakerDeck.embed(ws)(url)
    case _ => Future(embed(url, default))
  }

  private val timeout = 10000.millis
  private val resolveCache = mutable.WeakHashMap.empty[String, String]
  def resolve(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[String] = {
    def resolveRec(url: String, origin: String)(implicit ec: ExecutionContext): Future[String] = {
      resolveCache.get(url).map(res => Future(res)).getOrElse {
        try {
          ws.url(url).withFollowRedirects(false).withRequestTimeout(timeout).head().flatMap { response =>
            response.header("Location").filter(_.startsWith("http")).map { redirect =>
              resolveCache.put(origin, redirect)
              resolveRec(redirect, origin)
            }.getOrElse {
              resolveCache.put(origin, url)
              Future(url)
            }
          }.recover {
            case e: Throwable => resolveCache.put(origin, url); url
          }
        } catch {
          case e: Throwable => resolveCache.put(origin, url); Future(url)
        }
      }
    }
    resolveRec(url, url)
  }

  private val hash = "([^/?&#]+)"
  private case class EmbedUrl(value: String) {
    override def toString: String = this.value
  }
  private case class EmbedCode(value: String) {
    override def toString: String = this.value
  }

  private object YouTube {
    val name = "YouTube"
    val url1 = "https?://www.youtube.com/watch\\?(?:[^=]+=[^&]+&)*v=([^&]+).*".r
    val url2 = s"https?://youtu.be/$hash.*".r
    def embedUrl(videoId: String): EmbedUrl = EmbedUrl(s"https://www.youtube.com/embed/$videoId")
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe width="560" height="315" src="$embedUrl" frameborder="0" allowfullscreen></iframe>""")
    def embed(url: String, videoId: String): EmbedData = EmbedData(url, name, embedUrl(videoId).value, embedCode(embedUrl(videoId)).value)
  }
  private object Dailymotion {
    val name = "Dailymotion"
    val url = s"https?://www.dailymotion.com/video/$hash.*".r
    def embedUrl(videoId: String): EmbedUrl = EmbedUrl(s"//www.dailymotion.com/embed/video/${videoId.split("_").headOption.getOrElse(videoId)}")
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe src="$embedUrl" width="560" height="315" frameborder="0" allowfullscreen></iframe>""")
    def embed(url: String, videoId: String): EmbedData = EmbedData(url, name, embedUrl(videoId).value, embedCode(embedUrl(videoId)).value)
  }
  private object Vimeo {
    val name = "Vimeo"
    val url = s"https?://vimeo.com/$hash.*".r
    def embedUrl(videoId: String): EmbedUrl = EmbedUrl(s"https://player.vimeo.com/video/$videoId")
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe src="$embedUrl" width="640" height="360" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>""")
    def embed(url: String, videoId: String): EmbedData = EmbedData(url, name, embedUrl(videoId).value, embedCode(embedUrl(videoId)).value)
  }
  private object GoogleSlides {
    val name = "GoogleSlides"
    val url = s"https?://docs.google.com/presentation/d/$hash.*".r
    def embedUrl(slidesId: String): EmbedUrl = EmbedUrl(s"https://docs.google.com/presentation/d/$slidesId/embed")
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe src="$embedUrl" width="960" height="569" frameborder="0" allowfullscreen="true" mozallowfullscreen="true" webkitallowfullscreen="true"></iframe>""")
    def embed(url: String, slidesId: String): EmbedData = EmbedData(url, name, embedUrl(slidesId).value, embedCode(embedUrl(slidesId)).value)
  }
  private object SlidesDotCom {
    val name = "SlidesDotCom"
    val url = s"https?://slides.com/$hash/$hash.*".r
    def embedUrl(user: String, slidesId: String): EmbedUrl = EmbedUrl(s"//slides.com/$user/$slidesId/embed?style=light")
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe src="$embedUrl" width="576" height="420" scrolling="no" frameborder="0" webkitallowfullscreen mozallowfullscreen allowfullscreen></iframe>""")
    def embed(url: String, user: String, slidesId: String): EmbedData = EmbedData(url, name, embedUrl(user, slidesId).value, embedCode(embedUrl(user, slidesId)).value)
  }
  private object SlideShare {
    val name = "SlideShare"
    val url = s"https?://[a-z]+.slideshare.net/$hash/$hash.*".r
    private val embedUrlRegex = "(?is).*<meta class=\"twitter_player\" value=\"([^\"]+)\" name=\"twitter:player\" />.*".r
    def embedUrl(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedUrl]] = ws.url(url).withRequestTimeout(timeout).get().map {
      _.body match {
        case embedUrlRegex(u) => Right(EmbedUrl(u))
        case _ => Left(ApiError.notFound(s"Embed data not found in $name page"))
      }
    }.recover {
      case e: Throwable => Left(ApiError.from(e))
    }
    def embedCode(embedUrl: EmbedUrl): EmbedCode = EmbedCode(s"""<iframe src="$embedUrl" width="595" height="485" frameborder="0" marginwidth="0" marginheight="0" scrolling="no" style="border:1px solid #CCC; border-width:1px; margin-bottom:5px; max-width: 100%;" allowfullscreen></iframe>""")
    def embed(url: String, embedUrl: EmbedUrl): EmbedData = EmbedData(url, name, embedUrl.value, embedCode(embedUrl).value)
    def embed(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedData]] = embedUrl(ws)(url).map { _.right.map { embedUrl => embed(url, embedUrl) } }
  }
  private object SpeakerDeck {
    val name = "SpeakerDeck"
    val url = s"https?://speakerdeck.com/$hash/$hash.*".r
    private val embedUrlRegex = "(?is).*<div class=\"speakerdeck-embed\" data-id=\"([^\"]+)\" data-ratio=\"([^\"]+)\"></div>.*".r
    def embedUrl(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[Either[ApiError, (String, String)]] = ws.url(url).withRequestTimeout(timeout).get().map {
      _.body match {
        case embedUrlRegex(embedId, embedRatio) => Right((embedId, embedRatio))
        case _ => Left(ApiError.notFound(s"Embed data not found in $name page"))
      }
    }.recover {
      case e: Throwable => Left(ApiError.from(e))
    }
    def embedCode(embedId: String, embedRatio: String): EmbedCode = EmbedCode(s"""<script async class="speakerdeck-embed" data-id="$embedId" data-ratio="$embedRatio" src="//speakerdeck.com/assets/embed.js"></script>""")
    def embed(url: String, embedId: String, embedRatio: String): EmbedData = EmbedData(url, name, url, embedCode(embedId, embedRatio).value)
    def embed(ws: WSClient)(url: String)(implicit ec: ExecutionContext): Future[Either[ApiError, EmbedData]] = embedUrl(ws)(url).map { _.right.map { case (embedId, embedRatio) => embed(url, embedId, embedRatio) } }
  }
  private object Pdf {
    val name = "Pdf"
    val url = s".*\\.pdf".r
    def embedCode(url: String): EmbedCode = EmbedCode(s"""<object data="$url" type="application/pdf" width="640" height="480">alt : <a href="$url">$url</a></object>""")
    def embed(url: String): EmbedData = EmbedData(url, name, url, embedCode(url).value)
  }
}