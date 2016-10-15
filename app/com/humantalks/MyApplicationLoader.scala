package com.humantalks

import com.humantalks.auth.infrastructure.{ UserRepository, CredentialsRepository, AuthTokenRepository }
import com.humantalks.auth.services.{ AuthSrv, MailerSrv }
import com.humantalks.auth.silhouette._
import com.humantalks.common.Conf
import com.humantalks.common.services.EmbedSrv
import com.humantalks.internal.meetups.{ MeetupDbService, MeetupRepository, MeetupCtrl, MeetupApi }
import com.humantalks.internal.persons.{ PersonDbService, PersonRepository, PersonCtrl, PersonApi }
import com.humantalks.internal.talks.{ TalkDbService, TalkRepository, TalkCtrl, TalkApi }
import com.humantalks.internal.users.UserCtrl
import com.humantalks.tools.EmbedCtrl
import com.humantalks.tools.scrapers.TwitterScraper
import com.humantalks.internal.venues.{ VenueDbService, VenueRepository, VenueCtrl, VenueApi }
import global.Contexts
import global.infrastructure.Mongo
import play.api.cache.EhCacheComponents
import play.api.i18n.I18nComponents
import play.api.inject.{ NewInstanceInjector, SimpleInjector }
import play.api.libs.mailer.MailerComponents
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
    with MailerComponents
    with SilhouetteComponents {
  val conf = Conf(configuration)
  val silhouetteConf = SilhouetteConf(configuration)
  val ctx = Contexts(actorSystem)

  val embedSrv = EmbedSrv(wsClient)

  val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(configuration, applicationLifecycle)
  val mongo = Mongo(ctx, reactiveMongoApi)
  val userRepository = UserRepository(conf, ctx, mongo)
  val credentialsRepository = CredentialsRepository(conf, ctx, mongo)
  val authTokenRepository = AuthTokenRepository(conf, ctx, mongo)
  val venueRepository = VenueRepository(conf, ctx, mongo)
  val personRepository = PersonRepository(conf, ctx, mongo)
  val talkRepository = TalkRepository(conf, ctx, mongo, embedSrv)
  val meetupRepository = MeetupRepository(conf, ctx, mongo)

  val venueDbService = VenueDbService(meetupRepository, venueRepository)
  val personDbService = PersonDbService(personRepository, talkRepository)
  val talkDbService = TalkDbService(meetupRepository, talkRepository)
  val meetupDbService = MeetupDbService(meetupRepository)

  val authSrv = AuthSrv(passwordHasherRegistry, credentialsProvider, authInfoRepository)
  val mailerSrv = MailerSrv(mailerClient)

  implicit val messagesApiImp = messagesApi
  val router: Router = new Routes(
    httpErrorHandler,
    new com.humantalks.exposed.Application(ctx),
    new com.humantalks.auth.AuthCtrl(ctx, silhouette, silhouetteConf, authSrv, userRepository, credentialsRepository, authTokenRepository, avatarService, mailerSrv),
    new com.humantalks.internal.Application(ctx, silhouette),
    new VenueCtrl(ctx, silhouette, venueDbService, personDbService, meetupDbService),
    new PersonCtrl(ctx, silhouette, personDbService, talkDbService),
    new TalkCtrl(ctx, silhouette, personDbService, talkDbService, meetupDbService),
    new MeetupCtrl(ctx, silhouette, venueDbService, personDbService, talkDbService, meetupDbService),
    new UserCtrl(ctx, silhouette, userRepository),
    new VenueApi(ctx, silhouette, venueDbService),
    new PersonApi(ctx, silhouette, personDbService),
    new TalkApi(ctx, silhouette, talkDbService),
    new MeetupApi(ctx, silhouette, meetupDbService),
    new com.humantalks.tools.Application(ctx),
    new EmbedCtrl(ctx, embedSrv),
    new TwitterScraper(ctx, wsClient),
    new _root_.global.controllers.Application(ctx, mongo),
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