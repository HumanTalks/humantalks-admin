package com.humantalks.persons

import com.humantalks.common.helpers.ApiHelper
import global.Contexts
import play.api.mvc._

case class PersonApi(ctx: Contexts, personRepository: PersonRepository) extends Controller {
  import Contexts.wsToEC
  import ctx._

  def find = ApiHelper.findAction(personRepository)
  def create = ApiHelper.createAction(personRepository)
  def get(id: Person.Id) = ApiHelper.getAction(personRepository)(id)
  def update(id: Person.Id) = ApiHelper.updateAction(personRepository)(id)
  def delete(id: Person.Id) = ApiHelper.deleteAction(personRepository)(id)
}
