package com.humantalks

import com.humantalks.auth.silhouette._
import com.humantalks.common.Conf
import com.humantalks.common.services.EmbedSrv
import com.humantalks.meetups.{ MeetupDbService, MeetupRepository, MeetupCtrl, MeetupApi }
import com.humantalks.persons.{ PersonDbService, PersonRepository, PersonCtrl, PersonApi }
import com.humantalks.talks.{ TalkDbService, TalkRepository, TalkCtrl, TalkApi }
import com.humantalks.tools.EmbedCtrl
import com.humantalks.tools.scrapers.TwitterScraper
import com.humantalks.venues.{ VenueDbService, VenueRepository, VenueCtrl, VenueApi }
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
    with ReactiveMongoComponents {
  val conf = Conf(configuration)
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

  val userService = UserService(userRepository)
  //val authTokenService = AuthTokenRepository(authTokenService, clock)
  //val authEnv =
  //val silhouette = new SilhouetteProvider()

  val venueDbService = VenueDbService(meetupRepository, venueRepository)
  val personDbService = PersonDbService(personRepository, talkRepository)
  val talkDbService = TalkDbService(meetupRepository, talkRepository)
  val meetupDbService = MeetupDbService(meetupRepository)

  implicit val messagesApiImp = messagesApi
  val router: Router = new Routes(
    httpErrorHandler,
    new com.humantalks.common.controllers.Application(ctx),
    //new com.humantalks.auth.AuthCtrl(silhouette),
    new VenueCtrl(ctx, meetupRepository, venueDbService),
    new PersonCtrl(ctx, talkRepository, personDbService),
    new TalkCtrl(ctx, meetupRepository, personRepository, talkDbService),
    new MeetupCtrl(ctx, talkRepository, personRepository, venueRepository, meetupDbService),
    new VenueApi(ctx, venueRepository),
    new PersonApi(ctx, personRepository),
    new TalkApi(ctx, talkRepository),
    new MeetupApi(ctx, meetupRepository),
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

  // cf https://github.com/mohiva/play-silhouette-seed/blob/master/app/modules/SilhouetteModule.scala
  /*private object SilhouetteBuilder {
    import com.mohiva.play.silhouette.api.{Environment, Silhouette, SilhouetteProvider}
    import com.mohiva.play.silhouette.api.EventBus
    import com.mohiva.play.silhouette.api.actions.{UserAwareAction, UnsecuredAction, SecuredAction}
    import com.mohiva.play.silhouette.api.crypto.{CrypterAuthenticatorEncoder, Crypter, CookieSigner}
    import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
    import com.mohiva.play.silhouette.api.services.{AuthenticatorService, AvatarService}
    import com.mohiva.play.silhouette.api.util.{Clock, FingerprintGenerator, IDGenerator, PasswordHasherRegistry, PasswordHasher, HTTPLayer, PlayHTTPLayer, PasswordInfo}
    import com.mohiva.play.silhouette.crypto.{JcaCookieSigner, JcaCookieSignerSettings, JcaCrypter, JcaCrypterSettings}
    import com.mohiva.play.silhouette.impl.authenticators.{CookieAuthenticatorSettings, CookieAuthenticatorService, CookieAuthenticator}
    import com.mohiva.play.silhouette.impl.providers.{SocialProviderRegistry, CredentialsProvider}
    import com.mohiva.play.silhouette.impl.services.GravatarService
    import com.mohiva.play.silhouette.impl.util.{SecureRandomIDGenerator, DefaultFingerprintGenerator}
    import com.mohiva.play.silhouette.password.BCryptPasswordHasher
    import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
    import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository

    val clock: Clock = Clock()
    val userService: UserService = UserService(userRepository)
    val authTokenService: AuthTokenService = AuthTokenService(authTokenRepository, clock)
    val cookieSigner: CookieSigner = provideAuthenticatorCookieSigner(configuration)
    val crypter: Crypter = provideAuthenticatorCrypter(configuration)
    val fingerprintGenerator: FingerprintGenerator = new DefaultFingerprintGenerator(false)
    val idGenerator: IDGenerator = new SecureRandomIDGenerator()
    val authenticatorService: AuthenticatorService[CookieAuthenticator] = provideAuthenticatorService(cookieSigner, crypter, fingerprintGenerator, idGenerator, configuration, clock)
    val eventBus: EventBus = EventBus()
    val env: Environment[DefaultEnv] = Environment[DefaultEnv](userService, authenticatorService, Seq(), eventBus)
    val securedAction: SecuredAction = ???
    val unsecuredAction: UnsecuredAction = ???
    val userAwareAction: UserAwareAction = ???
    val silhouette: Silhouette[DefaultEnv] = new SilhouetteProvider(env, securedAction, unsecuredAction, userAwareAction)

    val passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo] = credentialsRepository
    val authInfoRepository: AuthInfoRepository = new DelegableAuthInfoRepository(passwordInfoDAO)
    val httpLayer: HTTPLayer = new PlayHTTPLayer(wsClient)
    val avatarService: AvatarService = new GravatarService(httpLayer)
    val passwordHasher: PasswordHasher = new BCryptPasswordHasher
    val passwordHasherRegistry: PasswordHasherRegistry = new PasswordHasherRegistry(passwordHasher)
    val credentialsProvider: CredentialsProvider = new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
    val socialProviderRegistry: SocialProviderRegistry = SocialProviderRegistry(Seq())
    val mailerClient: MailerClient = ???

    val controller = new com.humantalks.auth.AuthCtrl(configuration, ctx, silhouette, userService, authInfoRepository, authTokenService, avatarService, passwordHasherRegistry, credentialsProvider, socialProviderRegistry, mailerClient, clock)

    def provideAuthenticatorCookieSigner(configuration: Configuration): CookieSigner = {
      val config = configuration.underlying.as[JcaCookieSignerSettings]("silhouette.authenticator.cookie.signer")
      new JcaCookieSigner(config)
    }
    def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
      val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")
      new JcaCrypter(config)
    }
    def provideAuthenticatorService(cookieSigner: CookieSigner, crypter: Crypter, fingerprintGenerator: FingerprintGenerator, idGenerator: IDGenerator, configuration: Configuration, clock: Clock): AuthenticatorService[CookieAuthenticator] = {
      val config = configuration.underlying.as[CookieAuthenticatorSettings]("silhouette.authenticator")
      val encoder = new CrypterAuthenticatorEncoder(crypter)
      new CookieAuthenticatorService(config, None, cookieSigner, encoder, fingerprintGenerator, idGenerator, clock)
    }
  }*/
}
object MyComponents {
  def default = new MyComponents(ApplicationLoader.createContext(Environment.simple()))
}