package com.humantalks.common.values

import com.humantalks.auth.silhouette.User
import org.joda.time.DateTime
import play.api.libs.json.Json

case class Meta(
    created: DateTime,
    createdBy: User.Id,
    updated: DateTime,
    updatedBy: User.Id
) {
  def update(by: User.Id): Meta = Meta(this.created, this.createdBy, new DateTime(), by)
}
object Meta {
  implicit val format = Json.format[Meta]
}
