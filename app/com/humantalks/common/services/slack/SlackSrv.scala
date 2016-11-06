package com.humantalks.common.services.slack

import com.humantalks.common.Conf
import global.Contexts
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class SlackSrv(conf: Conf, ctx: Contexts, ws: WSClient) {
  import Contexts.wsToEC
  import ctx._

  // see https://api.slack.com/methods/channels.create
  // responses:
  //  {"ok":true,"channel":{"id":"C2YSR38GK","name":"test2","is_channel":true,"created":1478444007,"creator":"U0B0RS205","is_archived":false,"is_general":false,"is_member":true,"last_read":"0000000000.000000","latest":null,"unread_count":0,"unread_count_display":0,"members":["U0B0RS205"],"topic":{"value":"","creator":"","last_set":0},"purpose":{"value":"","creator":"","last_set":0}}}
  //  {"ok":false,"error":"name_taken"}
  def createChannel(name: String): Future[Either[String, Channel]] =
    ws.url(s"https://slack.com/api/channels.create").withQueryString(
      "token" -> conf.Slack.token,
      "name" -> name
    ).get().map(res => parseResponse(res.json, json => (json \ "channel").as[Channel]))

  // see https://api.slack.com/methods/channels.list
  def listChannels(): Future[Either[String, List[Channel]]] =
    ws.url(s"https://slack.com/api/channels.list").withQueryString(
      "token" -> conf.Slack.token,
      "exclude_archived" -> "0"
    ).get().map(res => parseResponse(res.json, json => (json \ "channels").as[List[Channel]]))

  def findChannel(name: String): Future[Either[String, Option[Channel]]] =
    listChannels().map(_.right.map(_.find(_.name == name)))

  def createChannelIfNotExists(name: String): Future[Either[String, Channel]] =
    findChannel(name).flatMap {
      case Right(channelOpt) => channelOpt.map(c => Future.successful(Right(c))).getOrElse {
        createChannel(name)
      }
      case Left(err) => Future.successful(Left(err))
    }

  // see https://api.slack.com/methods/chat.postMessage
  // responses:
  //  {"ok":true,"channel":"C2XSA7S49","ts":"1478364758.000002","message":{"text":"test","username":"Backend bot (local)","bot_id":"B2Z8THLKY","type":"message","subtype":"bot_message","ts":"1478364758.000002"}}
  //  {"ok":false,"error":"channel_not_found"}
  def postMessage(channel: String, text: String, attachments: Option[JsArray]): Future[Either[String, Message]] =
    ws.url(s"https://slack.com/api/chat.postMessage").withQueryString(
      "token" -> conf.Slack.token,
      "channel" -> (if (conf.App.isProd) channel else "test"),
      "as_user" -> "false",
      "username" -> (conf.Slack.botName + (if (conf.App.isProd) "" else " (" + conf.App.env + " / #" + channel + ")")),
      "icon_url" -> conf.Slack.botIcon,
      "text" -> text,
      "attachments" -> attachments.map(arr => Json.stringify(arr)).getOrElse(""),
      "mrkdwn" -> "true"
    ).get().map(res => parseResponse(res.json, json => (json \ "message").as[Message]))

  def postMessageAndCreateChannelIfNeeded(channel: String, text: String, attachments: Option[JsArray]): Future[Either[String, Message]] =
    createChannelIfNotExists(channel).flatMap {
      case Right(_) => postMessage(channel, text, attachments)
      case Left(err) => Future.successful(Left(err))
    }

  private def parseResponse[T](json: JsValue, parse: JsValue => T): Either[String, T] =
    (json \ "error").asOpt[String].map(err => Left(err)).getOrElse {
      Right(parse(json))
    }
}
