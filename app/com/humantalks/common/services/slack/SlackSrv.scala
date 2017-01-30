package com.humantalks.common.services.slack

import com.humantalks.common.Conf
import com.humantalks.common.services.slack.models.{ SlackUser, SlackMessage, SlackChannel }
import global.Contexts
import play.api.libs.json.{ JsArray, JsValue, Json }
import play.api.libs.ws.WSClient

import scala.concurrent.Future

case class SlackSrv(conf: Conf, ctx: Contexts, ws: WSClient) {
  import Contexts.wsToEC
  import ctx._
  private val baseUrl = "https://slack.com/api"

  // see https://api.slack.com/methods/channels.create
  def createChannel(name: String): Future[Either[String, SlackChannel]] =
    ws.url(s"$baseUrl/channels.create").withQueryString(
      "token" -> conf.Slack.token,
      "name" -> name
    ).get().map(res => parseResponse(res.json, json => (json \ "channel").as[SlackChannel]))

  // see https://api.slack.com/methods/channels.list
  def listChannels(): Future[Either[String, List[SlackChannel]]] =
    ws.url(s"$baseUrl/channels.list").withQueryString(
      "token" -> conf.Slack.token,
      "exclude_archived" -> "0"
    ).get().map(res => parseResponse(res.json, json => (json \ "channels").as[List[SlackChannel]]))

  def findChannel(name: String): Future[Either[String, Option[SlackChannel]]] =
    listChannels().map(_.right.map(_.find(_.name == name)))

  // https://api.slack.com/methods/channels.invite
  def channelInvite(channelId: String, userId: String): Future[Either[String, SlackChannel]] =
    ws.url(s"$baseUrl/channels.invite").withQueryString(
      "token" -> conf.Slack.token,
      "channel" -> channelId,
      "user" -> userId
    ).get().map(res => parseResponse(res.json, json => (json \ "channel").as[SlackChannel]))

  // see https://api.slack.com/methods/users.list
  def listUsers(): Future[Either[String, List[SlackUser]]] =
    ws.url(s"$baseUrl/users.list").withQueryString(
      "token" -> conf.Slack.token
    ).get().map(res => parseResponse(res.json, json => (json \ "members").as[List[SlackUser]]))

  // see https://api.slack.com/methods/chat.postMessage
  def postMessage(channel: String, text: String, attachments: Option[JsArray]): Future[Either[String, SlackMessage]] =
    ws.url(s"$baseUrl/chat.postMessage").withQueryString(
      "token" -> conf.Slack.token,
      "channel" -> (if (conf.App.isProd) channel else "test"),
      "as_user" -> "false",
      "username" -> (conf.Slack.botName + (if (conf.App.isProd) "" else " (" + conf.App.env + " / #" + channel + ")")),
      "icon_url" -> conf.Slack.botIcon,
      "text" -> text,
      "attachments" -> attachments.map(arr => Json.stringify(arr)).getOrElse(""),
      "mrkdwn" -> "true"
    ).get().map(res => parseResponse(res.json, json => (json \ "message").as[SlackMessage]))

  def createChannelAndInviteAllUsers(name: String): Future[Either[String, SlackChannel]] =
    for {
      Right(channel) <- createChannel(name)
      Right(users) <- listUsers()
      res <- Future.sequence(users.filterNot(u => u.deleted || u.name == "slackbot").map(user => channelInvite(channel.id, user.id)))
    } yield Right(channel)

  def createChannelIfNotExists(name: String): Future[Either[String, SlackChannel]] =
    findChannel(name).flatMap {
      case Right(channelOpt) => channelOpt.map(c => Future.successful(Right(c))).getOrElse {
        createChannelAndInviteAllUsers(name)
      }
      case Left(err) => Future.successful(Left(err))
    }

  def postMessageAndCreateChannelIfNeeded(channel: String, text: String, attachments: Option[JsArray]): Future[Either[String, SlackMessage]] =
    createChannelIfNotExists(channel).flatMap {
      case Right(_) => postMessage(channel, text, attachments)
      case Left(err) => Future.successful(Left(err))
    }

  private def parseResponse[T](json: JsValue, parse: JsValue => T): Either[String, T] =
    (json \ "error").asOpt[String].map(err => Left(err)).getOrElse {
      Right(parse(json))
    }
}
