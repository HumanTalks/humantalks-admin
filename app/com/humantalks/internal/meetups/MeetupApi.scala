package com.humantalks.internal.meetups

import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class MeetupApi(ctx: Contexts, meetupRepository: MeetupRepository) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = ApiHelper.findAction(meetupRepository)
  def create = ApiHelper.createAction(meetupRepository)
  def get(id: Meetup.Id) = ApiHelper.getAction(meetupRepository)(id)
  def update(id: Meetup.Id) = ApiHelper.updateAction(meetupRepository)(id)
  def delete(id: Meetup.Id) = ApiHelper.deleteAction(meetupRepository)(id)
}