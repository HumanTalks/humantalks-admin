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
    ref = "meetup.event.description",
    content = ContentType.Html,
    description = """
      |Template pour créer la description sur meetup.
      |Format utilisable : <a href="https://www.meetup.com/fr-FR/meetup_api/docs/:urlname/events/?uri=%2Fmeetup_api%2Fdocs%2F%3Aurlname%2Fevents%2F#create" target="_blank">HTML autorisé par meetup</a>
    """.stripMargin.trim,
    value = """
      |{{#meetup}}
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
      |- <b>{{title}}</b> par {{#speakers}}<b>{{name}}</b>{{#twitter}} (<a href="https://twitter.com/{{twitter}}">@{{twitter}}</a>){{/twitter}} {{/speakers}}
      |
      |{{description}}
      |
      |{{/talks}}
      |---
      |
      |Proposez vos sujets pour les prochaines sessions : <a href="https://humantalksparis.herokuapp.com/submit-talk">https://humantalksparis.herokuapp.com/submit-talk</a>
    """.stripMargin.trim
  )
  val proposalSubmittedEmailSubject = Data(
    ref = "proposal.submitted.email.subject",
    content = ContentType.Text,
    description = """
        |Sujet du mail envoyé aux personnes qui proposent un talk via le formulaire
        """.stripMargin.trim,
    value = """
        |Thanks for submitting to HumanTalks Paris
      """.stripMargin.trim
  )
  val proposalSubmittedEmailContent = Data(
    ref = "proposal.submitted.email.content",
    content = ContentType.Html,
    description = """
        |Contenu du mail envoyé aux personnes qui proposent un talk via le formulaire
      """.stripMargin.trim,
    value = """
        |<html>
        |    <body>
        |        <p>Hello,</p>
        |        <p>
        |            Thanks for submitting your proposal <b>{{proposal.data.title}}</b>. We will come back to you soon.<br>
        |            If you have any question, feel free to reach us at {{emailOrga}}<br>
        |            Note that you can edit your proposal whenever you want using this link : <a href="{{proposalEditUrl}}">{{proposalEditUrl}}</a>
        |        </p>
        |    </body>
        |</html>
      """.stripMargin.trim
  )
  val proposalSubmittedSlackMessage = Data(
    ref = "proposal.submitted.slack.message",
    content = ContentType.Markdown,
    description = """
        |Message posté sur slack suite à la soumission d'une proposition via le formulaire
        |Format utilisable : <a href="https://api.slack.com/docs/message-formatting" target="_blank">Markdown à la sauce slack</a>
      """.stripMargin.trim,
    value = """
        |Nouvelle <{{proposalUrl}}|proposition de talk> par {{#speakers}}*{{name}}* {{/speakers}} :
      """.stripMargin.trim
  )
  val proposalSubmittedSlackTitle = Data(
    ref = "proposal.submitted.slack.title",
    content = ContentType.Text,
    description = """
        |Titre de la pièce jointe postée sur slack suite à la soumission d'une proposition via le formulaire
      """.stripMargin.trim,
    value = """
        |{{proposal.data.title}}
      """.stripMargin.trim
  )
  val proposalSubmittedSlackText = Data(
    ref = "proposal.submitted.slack.text",
    content = ContentType.Markdown,
    description = """
        |Contenu de la pièce jointe postée sur slack suite à la soumission d'une proposition via le formulaire
        |Format utilisable : <a href="https://api.slack.com/docs/message-formatting" target="_blank">Markdown à la sauce slack</a>
      """.stripMargin.trim,
    value = """
        |{{proposal.data.description}}
      """.stripMargin.trim
  )
  val meetupCreatedSlackMessage = Data(
    ref = "meetup.created.slack.message",
    content = ContentType.Markdown,
    description = """
        |Message posté sur slack dans le channel du mois lorsqu'un meetup est créé
        |Format utilisable : <a href="https://api.slack.com/docs/message-formatting" target="_blank">Markdown à la sauce slack</a>
      """.stripMargin.trim,
    value = """
        |Meetup <{{meetupUrl}}|{{meetup.title}}> créé !
      """.stripMargin.trim
  )
  val talkAddedToMeetupSlackMessage = Data(
    ref = "talk.added.to.meetup.slack.message",
    content = ContentType.Markdown,
    description = """
        |Message posté dans le channel correspondant au meetup lorsqu'un talk est ajouté à un meetup
        |Format utilisable : <a href="https://api.slack.com/docs/message-formatting" target="_blank">Markdown à la sauce slack</a>
      """.stripMargin.trim,
    value = """
        |Talk ajouté par {{addedBy.data.name}} pour les <{{meetupUrl}}|{{meetup.data.title}}> : {{#talk}}*{{title}}* par {{#speakers}}*{{name}}* {{/speakers}}{{/talk}}
      """.stripMargin.trim
  )
  val defaultConfigs = List(
    meetupEventDescription,
    proposalSubmittedEmailSubject,
    proposalSubmittedEmailContent,
    proposalSubmittedSlackMessage,
    proposalSubmittedSlackTitle,
    proposalSubmittedSlackText,
    meetupCreatedSlackMessage,
    talkAddedToMeetupSlackMessage
  )
}
