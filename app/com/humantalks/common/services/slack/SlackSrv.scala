package com.humantalks.common.services.slack

import com.humantalks.common.Conf
import global.Contexts
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class SlackSrv(conf: Conf, ctx: Contexts, ws: WSClient) {
  import Contexts.wsToEC
  import ctx._

  // see https://api.slack.com/methods/chat.postMessage
  // TODO : work on return type...
  // responses:
  //  {"ok":true,"channel":"C2XSA7S49","ts":"1478364758.000002","message":{"text":"test","username":"Backend bot (local)","bot_id":"B2Z8THLKY","type":"message","subtype":"bot_message","ts":"1478364758.000002"}}
  //  {"ok":false,"error":"channel_not_found"}
  def postMessage(channel: String, text: String, attachments: Option[JsArray]): Future[Boolean] = {
    ws.url(s"https://slack.com/api/chat.postMessage").withQueryString(
      "token" -> conf.Slack.token,
      "channel" -> (if (conf.App.isProd) channel else "test"),
      "as_user" -> "false",
      "username" -> (conf.Slack.botName + (if (conf.App.isProd) "" else " (" + conf.App.env + " / #"+channel+")")),
      "icon_url" -> conf.Slack.botIcon,
      "text" -> text,
      "attachments" -> attachments.map(arr => Json.stringify(arr)).getOrElse("")
    ).get().map { res =>
        (res.json \ "ok").as[Boolean]
      }
  }

  // see https://api.slack.com/methods/files.upload
  def uploadFile(): Future[JsValue] = {
    ws.url(s"https://slack.com/api/files.upload").withQueryString(
      "token" -> conf.Slack.token,
      "channels" -> "test",
      "filename" -> "Comment créer un jeu vidéo en 5 minutes.txt",
      "title" -> "Comment créer un jeu vidéo en 5 minutes",
      "content" -> "J'ai créé une quinzaine de jeux 2D avec différents frameworks. - Et j'ai récemment décidé de créer mon propre framework open source pour développer des jeux beaucoup plus vite. - Le talk est pour presenter rapidement le framework, et de montrer qu'il est très simple de faire un petit jeux HTML5 en 5 minutes. Preview du framework : lessmilk.com/milkyJS/"
    ).get().map { res =>
        res.json
      }
  }
}
