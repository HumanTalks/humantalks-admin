package com.humantalks

import com.humantalks.common.Conf
import com.humantalks.infrastructure.{ TalkRepository, MeetupRepository, VenueRepository, PersonRepository }
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
  val reactiveMongoApi: ReactiveMongoApi = new DefaultReactiveMongoApi(configuration, applicationLifecycle)
  val mongo = Mongo(ctx, reactiveMongoApi)
  val personRepository = PersonRepository(conf, ctx, mongo)
  val venueRepository = VenueRepository(conf, ctx, mongo)
  val talkRepository = TalkRepository(conf, ctx, mongo)
  val meetupRepository = MeetupRepository(conf, ctx, mongo)

  implicit val messagesApiImp = messagesApi
  val router: Router = new Routes(
    httpErrorHandler,
    new com.humantalks.controllers.html.Application(ctx),
    new com.humantalks.controllers.html.Persons(ctx, personRepository),
    new com.humantalks.controllers.html.Venues(ctx, venueRepository),
    new com.humantalks.controllers.html.Talks(ctx, talkRepository, personRepository),
    new com.humantalks.controllers.html.Meetups(ctx, meetupRepository, venueRepository),
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