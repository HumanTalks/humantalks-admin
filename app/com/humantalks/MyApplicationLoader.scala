package com.humantalks

import com.humantalks.auth.infrastructure.{ CredentialsRepository, AuthTokenRepository }
import com.humantalks.auth.services.{ AuthSrv, MailerSrv }
import com.humantalks.auth.silhouette._
import com.humantalks.common.Conf
import com.humantalks.common.services.EmbedSrv
import com.humantalks.common.services.sendgrid.SendgridSrv
import com.humantalks.exposed.PublicApi
import com.humantalks.exposed.proposals.{ ProposalRepository, ProposalDbService }
import com.humantalks.internal.admin.AdminCtrl
import com.humantalks.internal.meetups.{ MeetupDbService, MeetupRepository, MeetupCtrl, MeetupApi }
import com.humantalks.internal.persons.{ PersonDbService, PersonRepository, PersonCtrl, PersonApi }
import com.humantalks.internal.proposals.ProposalCtrl
import com.humantalks.internal.talks.{ TalkDbService, TalkRepository, TalkCtrl, TalkApi }
import com.humantalks.tools.EmbedCtrl
import com.humantalks.tools.scrapers.TwitterScraper
import com.humantalks.internal.venues.{ VenueDbService, VenueRepository, VenueCtrl, VenueApi }
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
  val venueRepository = VenueRepository(conf, ctx, mongo)
  val personRepository = PersonRepository(conf, ctx, mongo)
  val talkRepository = TalkRepository(conf, ctx, mongo, embedSrv)
  val meetupRepository = MeetupRepository(conf, ctx, mongo)
  val proposalRepository = ProposalRepository(conf, ctx, mongo, embedSrv)

  val venueDbService = VenueDbService(venueRepository, meetupRepository)
  val personDbService = PersonDbService(personRepository, talkRepository, proposalRepository)
  val talkDbService = TalkDbService(talkRepository, meetupRepository, proposalRepository)
  val meetupDbService = MeetupDbService(talkRepository, meetupRepository)
  val proposalDbService = ProposalDbService(talkRepository, proposalRepository)

  val authSrv = AuthSrv(passwordHasherRegistry, credentialsProvider, authInfoRepository)
  val sendgridSrv = SendgridSrv(conf, wsClient)
  val mailerSrv = MailerSrv(conf, sendgridSrv)

  implicit val messagesApiImp = messagesApi
  val router: Router = new Routes(
    httpErrorHandler,
    com.humantalks.exposed.Application(ctx),
    com.humantalks.exposed.proposals.ProposalCtrl(conf, ctx, personDbService, talkDbService, proposalDbService, sendgridSrv),
    com.humantalks.auth.AuthCtrl(ctx, silhouette, conf, authSrv, personRepository, credentialsRepository, authTokenRepository, avatarService, mailerSrv),
    com.humantalks.internal.Application(ctx, silhouette),
    VenueCtrl(ctx, silhouette, venueDbService, personDbService, meetupDbService),
    PersonCtrl(ctx, silhouette, personDbService, talkDbService, proposalDbService),
    TalkCtrl(ctx, silhouette, personDbService, talkDbService, meetupDbService),
    MeetupCtrl(ctx, silhouette, venueDbService, personDbService, talkDbService, meetupDbService, proposalDbService),
    ProposalCtrl(ctx, silhouette, personDbService, proposalDbService),
    AdminCtrl(ctx, silhouette, personDbService, credentialsRepository, authTokenRepository),
    PublicApi(ctx, venueDbService, personDbService, talkDbService, meetupDbService),
    VenueApi(ctx, silhouette, venueDbService),
    PersonApi(ctx, silhouette, personDbService),
    TalkApi(ctx, silhouette, talkDbService),
    MeetupApi(ctx, silhouette, meetupDbService),
    com.humantalks.tools.Application(ctx),
    EmbedCtrl(ctx, embedSrv),
    TwitterScraper(ctx, wsClient),
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