package com.humantalks.exposed.entities

import com.humantalks.internal.events.Event
import play.api.libs.json.Json

case class PublicAttendee(
  event: Event.Id,
  id: Long,
  name: String,
  avatar: Option[String],
  checkin: Boolean
)
object PublicAttendee {
  def from(attendee: PublicAttendee): PublicAttendee = PublicAttendee(
    event = attendee.event,
    id = attendee.id,
    name = attendee.name,
    avatar = attendee.avatar,
    checkin = attendee.checkin
  )

  implicit val format = Json.format[PublicAttendee]
}
