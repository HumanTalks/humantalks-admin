package com.humantalks.internal.talks

import global.Contexts
import global.helpers.ApiHelper
import play.api.mvc.Controller

case class TalkApi(ctx: Contexts, talkRepository: TalkRepository) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = ApiHelper.findAction(talkRepository)
  def create = ApiHelper.createAction(talkRepository)
  def get(id: Talk.Id) = ApiHelper.getAction(talkRepository)(id)
  def update(id: Talk.Id) = ApiHelper.updateAction(talkRepository)(id)
  def delete(id: Talk.Id) = ApiHelper.deleteAction(talkRepository)(id)
}
