package com.humantalks.internal.persons

import global.Contexts
import global.helpers.ApiHelper
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
