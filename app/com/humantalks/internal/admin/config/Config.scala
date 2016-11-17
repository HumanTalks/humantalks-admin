package com.humantalks.internal.admin.config

import com.humantalks.common.values.Meta
import global.helpers.EnumerationHelper
import global.values.{ TypedIdHelper, TypedId }
import play.api.data.Forms._
import play.api.libs.json.Json

case class Config(
  id: Config.Id,
  data: Config.Data,
  meta: Meta
)
object Config {
  case class Id(value: String) extends TypedId(value)
  object Id extends TypedIdHelper[Id] {
    def from(value: String): Either[String, Id] = TypedId.from(value, "Config.Id").right.map(Id(_))
    def generate(): Id = Id(TypedId.generate())
  }

  case class Data(
      ref: String,
      content: ContentType.Value,
      description: String,
      value: String
  ) {
    def trim: Data = this.copy(
      ref = ref.trim,
      description = description.trim,
      value = value.trim
    )
  }

  object ContentType extends Enumeration {
    val Text, Markdown, Html = Value
  }
  implicit val mappingContentType = EnumerationHelper.formMapping(ContentType)
  implicit val formatContentType = EnumerationHelper.enumFormat(ContentType)
  implicit val formatData = Json.format[Data]
  implicit val format = Json.format[Config]
  val fields = mapping(
    "ref" -> nonEmptyText,
    "content" -> of[ContentType.Value],
    "description" -> nonEmptyText,
    "value" -> nonEmptyText
  )(Data.apply)(Data.unapply)

  /* Default values for configs */

  val meetupEventDescription = Data(
    "meetup.event.description",
    ContentType.Html,
    """Template pour créer la description sur meetup.
      |
      |Format utilisable : <a href="https://www.meetup.com/fr-FR/meetup_api/docs/:urlname/events/?uri=%2Fmeetup_api%2Fdocs%2F%3Aurlname%2Fevents%2F#create" target="_blank">sous ensemble de HTML</a>
      |
      |Données disponibles :
      | - <a href="https://humantalksparis.herokuapp.com/api/meetups/6f483568-bd46-4760-88f9-75e88d3b8b79" target="_blank">meetup</a>
      | - <a href="https://humantalksparis.herokuapp.com/api/venues/4e7ff2ef-94d1-4ace-82d7-31b3e746e43a" target="_blank">venue</a>
      | - <a href="https://humantalksparis.herokuapp.com/api/talks?include=speaker" target="_blank">talks</a>
    """.stripMargin.trim,
    """{{#meetup}}
      |{{meetup.description}}
      |{{/meetup}}
      |
      |{{#venue}}
      |Ce mois-ci nous sommes chez {{venue.name}}, merci à eux de nous accueillir dans leurs locaux :)
      |
      |{{venue.logo}}
      |{{/venue}}
      |
      |{{#talks}}
      |- <b>{{title}}</b> par {{#speakers}}<b>{{name}}</b>{{#twitter}} (<a href="https://twitter.com/{{twitter}}">@{{twitter}}</a>){{/twitter}}{{/speakers}}
      |
      |{{description}}
      |
      |{{/talks}}
      |---
      |
      |Proposez vos sujets pour les prochaines sessions : <a href="https://humantalksparis.herokuapp.com/submit-talk">https://humantalksparis.herokuapp.com/submit-talk</a>
    """.stripMargin.trim
  )
}
