package com.humantalks.common.services.sendgrid

import com.humantalks.common.Conf
import play.api.libs.json.Json
import play.api.libs.ws.{ WSResponse, WSClient }

import scala.concurrent.Future

case class SendgridSrv(conf: Conf, ws: WSClient) {
  val baseUrl = "https://api.sendgrid.com/v3"

  def send(email: Email): Future[WSResponse] =
    ws.url(baseUrl + "/mail/send")
      .withHeaders(
        "Authorization" -> s"Bearer ${conf.Sendgrid.apiKey}",
        "Content-Type" -> "application/json"
      )
      .post(Json.toJson(email))
}
