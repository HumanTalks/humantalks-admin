package com.humantalks.meetups

import com.humantalks.common.helpers.ApiHelper
import global.Contexts
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
