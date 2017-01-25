package com.humantalks

import com.humantalks.auth.infrastructure.{ AuthTokenRepository, CredentialsRepository }
import com.humantalks.auth.services.{ AuthSrv, MailerSrv }
import com.humantalks.auth.silhouette._
import com.humantalks.common.Conf
import com.humantalks.common.controllers.Select2Ctrl
import com.humantalks.common.services.meetup.{ MeetupSrv, MeetupApi }
import com.humantalks.common.services.slack.SlackSrv
import com.humantalks.common.services.{ NotificationSrv, EmbedSrv }
import com.humantalks.common.services.sendgrid.SendgridSrv
import com.humantalks.exposed.PublicApi
import com.humantalks.internal.admin.AdminCtrl
import com.humantalks.internal.admin.config.{ ConfigApiCtrl, ConfigCtrl, ConfigDbService, ConfigRepository }
import com.humantalks.internal.events.{ EventApiCtrl, EventCtrl, EventDbService, EventRepository }
import com.humantalks.internal.persons.{ PersonApiCtrl, PersonCtrl, PersonDbService, PersonRepository }
import com.humantalks.internal.talks.{ TalkApiCtrl, TalkCtrl, TalkDbService, TalkRepository }
import com.humantalks.tools.EmbedCtrl
import com.humantalks.tools.scrapers.{ EmailScraper, TwitterScraper }
import com.humantalks.internal.partners.{ PartnerApiCtrl, PartnerCtrl, PartnerDbService, PartnerRepository }
import global.Contexts
import global.infrastructure.Mongo
import play.api.cache.EhCacheComponents
import play.api.i18n.I18nComponents
import play.api.inject.{ NewInstanceInjector, SimpleInjector }
import play.api.libs.ws.ahc.AhcWSComponents
import play.api.routing.Router
import play.api._
import play.filters.gzip.GzipFilterComponents
import play.modules.reactivemongo.{ DefaultReactiveMongoApi, ReactiveMongoApi, ReactiveMongoComponents }
import router.Routes

class MyApplicationLoader extends ApplicationLoader {
  def load(context: ApplicationLoader.Context) = {
    LoggerConfigurator(context.environment.classLoader).foreach {
      _.configure(context.environment)
    }
    new MyComponents(context).application
  }
}

class MyComponents(context: ApplicationLoader.Context)
    extends BuiltInComponentsFromContext(context)
    with I18nComponents
    with AhcWSComponents
    with EhCacheComponents
    with GzipFilterComponents
    with ReactiveMongoComponents
    with SilhouetteComponents {
  val conf = Conf(configuration)
  val ctx = Contexts(actorSystem)

  val embedSrv = EmbedSrv(wsClient)

  val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(configuration, applicationLifecycle)
  val mongo = Mongo(ctx, reactiveMongoApi)
  val credentialsRepository = CredentialsRepository(conf, ctx, mongo)
  val authTokenRepository = AuthTokenRepository(conf, ctx, mongo)
  val partnerRepository = PartnerRepository(conf, ctx, mongo)
  val personRepository = PersonRepository(conf, ctx, mongo)
  val talkRepository = TalkRepository(conf, ctx, mongo, embedSrv)
  val eventRepository = EventRepository(conf, ctx, mongo)
  val configRepository = ConfigRepository(conf, ctx, mongo)

  val partnerDbService = PartnerDbService(partnerRepository, eventRepository)
  val personDbService = PersonDbService(credentialsRepository, personRepository, talkRepository)
  val talkDbService = TalkDbService(talkRepository, eventRepository)
  val eventDbService = EventDbService(talkRepository, eventRepository)
  val configDbService = ConfigDbService(configRepository)

  val authSrv = AuthSrv(passwordHasherRegistry, credentialsProvider, authInfoRepository)

  val sendgridSrv = SendgridSrv(conf, wsClient)
  val mailerSrv = MailerSrv(conf, sendgridSrv)
  val slackSrv = SlackSrv(conf, ctx, wsClient)
  val meetupApi = MeetupApi(conf, ctx, wsClient)
  val meetupSrv = MeetupSrv(conf, ctx, meetupApi, partnerDbService, eventDbService, configDbService)
  val notificationSrv = NotificationSrv(conf, sendgridSrv, slackSrv, personDbService, talkDbService, eventDbService, configDbService)

  implicit val messagesApiImp = messagesApi
  val router: Router = new Routes(
    httpErrorHandler,
    com.humantalks.exposed.Application(ctx),
    com.humantalks.exposed.talks.TalkCtrl(conf, ctx, personDbService, talkDbService, notificationSrv),
    com.humantalks.auth.AuthCtrl(ctx, silhouette, conf, authSrv, personRepository, credentialsRepository, authTokenRepository, avatarService, mailerSrv),
    com.humantalks.internal.Application(ctx, silhouette),
    PartnerCtrl(ctx, silhouette, partnerDbService, personDbService, eventDbService),
    PersonCtrl(ctx, silhouette, personDbService, talkDbService),
    TalkCtrl(ctx, silhouette, personDbService, talkDbService, eventDbService),
    EventCtrl(ctx, silhouette, partnerDbService, personDbService, talkDbService, eventDbService, meetupSrv, notificationSrv),
    AdminCtrl(ctx, silhouette, personDbService, credentialsRepository, authTokenRepository),
    ConfigCtrl(ctx, silhouette, configDbService),
    PublicApi(ctx, partnerDbService, personDbService, talkDbService, eventDbService),
    Select2Ctrl(ctx, silhouette, partnerDbService, personDbService, talkDbService, eventDbService),
    PartnerApiCtrl(ctx, silhouette, partnerDbService),
    PersonApiCtrl(ctx, silhouette, personDbService),
    TalkApiCtrl(ctx, silhouette, talkDbService),
    EventApiCtrl(ctx, silhouette, eventDbService),
    ConfigApiCtrl(conf, ctx, silhouette, partnerDbService, personDbService, talkDbService, eventDbService, configDbService),
    com.humantalks.tools.Application(ctx),
    EmbedCtrl(ctx, embedSrv),
    TwitterScraper(ctx, wsClient),
    EmailScraper(ctx, wsClient),
    _root_.global.controllers.Application(ctx, mongo),
    new _root_.controllers.Assets(httpErrorHandler)
  )

  override lazy val injector = {
    new SimpleInjector(NewInstanceInjector) +
      router +
      cookieSigner +
      csrfTokenSigner +
      httpConfiguration +
      tempFileCreator +
      global +
      crypto +
      wsApi +
      messagesApi
  }
}
object MyComponents {
  def default = new MyComponents(ApplicationLoader.createContext(Environment.simple()))
}