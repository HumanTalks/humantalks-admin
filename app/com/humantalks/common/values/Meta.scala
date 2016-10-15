package com.humantalks.common.values

import com.humantalks.internal.persons.Person
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Meta(
    created: DateTime,
    createdBy: Person.Id,
    updated: DateTime,
    updatedBy: Person.Id
) {
  def update(by: Person.Id): Meta = Meta(this.created, this.createdBy, new DateTime(), by)
}
object Meta {
  implicit val format = Json.format[Meta]
  def from(by: Person.Id): Meta = Meta(new DateTime(), by, new DateTime(), by)
}
