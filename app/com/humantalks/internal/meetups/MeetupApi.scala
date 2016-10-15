package com.humantalks.internal.meetups

import com.humantalks.auth.silhouette.SilhouetteEnv
import com.mohiva.play.silhouette.api.Silhouette
import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class MeetupApi(
    ctx: Contexts,
    silhouette: Silhouette[SilhouetteEnv],
    meetupDbService: MeetupDbService
) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = silhouette.SecuredAction.async { implicit req => ApiHelper.find(meetupDbService) }
  def get(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.get(meetupDbService, id) }
  def create = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.create(meetupDbService, req.identity.id, req.body) }
  def update(id: Meetup.Id) = silhouette.SecuredAction.async(parse.json) { implicit req => ApiHelper.update(meetupDbService, req.identity.id, id, req.body) }
  def delete(id: Meetup.Id) = silhouette.SecuredAction.async { implicit req => ApiHelper.delete(meetupDbService, id) }
}
