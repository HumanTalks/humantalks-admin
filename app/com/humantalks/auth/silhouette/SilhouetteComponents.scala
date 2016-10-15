package com.humantalks.auth.silhouette

import com.humantalks.auth.infrastructure.CredentialsRepository
import com.humantalks.internal.persons.PersonRepository
import com.mohiva.play.silhouette.api.actions._
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.{ SilhouetteProvider, Silhouette, Environment, EventBus }
import com.mohiva.play.silhouette.api.crypto.{ CrypterAuthenticatorEncoder, Crypter, CookieSigner }
import com.mohiva.play.silhouette.api.services.{ AvatarService, AuthenticatorService }
import com.mohiva.play.silhouette.api.util._
import com.mohiva.play.silhouette.crypto.{ JcaCrypter, JcaCrypterSettings, JcaCookieSigner, JcaCookieSignerSettings }
import com.mohiva.play.silhouette.impl.authenticators.{ CookieAuthenticatorService, CookieAuthenticatorSettings, CookieAuthenticator }
import com.mohiva.play.silhouette.impl.providers.{ SocialProviderRegistry, CredentialsProvider }
import com.mohiva.play.silhouette.impl.services.GravatarService
import com.mohiva.play.silhouette.impl.util.{ SecureRandomIDGenerator, DefaultFingerprintGenerator }
import com.mohiva.play.silhouette.password.BCryptPasswordHasher
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import play.api.Configuration
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.Implicits._

// cf https://github.com/mohiva/play-silhouette-seed/blob/master/app/modules/SilhouetteModule.scala
trait SilhouetteComponents
    extends SecuredActionComponents
    with SecuredErrorHandlerComponents
    with UnsecuredActionComponents
    with UnsecuredErrorHandlerComponents
    with UserAwareActionComponents {
  // needed components
  val configuration: Configuration
  val wsClient: WSClient
  val personRepository: PersonRepository
  val credentialsRepository: CredentialsRepository

  // silhouette components
  private lazy val clock: Clock = Clock()
  private lazy val cookieSigner: CookieSigner = provideAuthenticatorCookieSigner(configuration)
  private lazy val crypter: Crypter = provideAuthenticatorCrypter(configuration)
  private lazy val fingerprintGenerator: FingerprintGenerator = new DefaultFingerprintGenerator(false)
  private lazy val idGenerator: IDGenerator = new SecureRandomIDGenerator()
  private lazy val authenticatorService: AuthenticatorService[CookieAuthenticator] = provideAuthenticatorService(cookieSigner, crypter, fingerprintGenerator, idGenerator, configuration, clock)
  private lazy val eventBus: EventBus = EventBus()
  private lazy val env: Environment[SilhouetteEnv] = Environment[SilhouetteEnv](personRepository, authenticatorService, Seq(), eventBus)

  private lazy val httpLayer: HTTPLayer = new PlayHTTPLayer(wsClient)
  private lazy val passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo] = credentialsRepository
  private lazy val passwordHasher: PasswordHasher = new BCryptPasswordHasher
  lazy val authInfoRepository: AuthInfoRepository = new DelegableAuthInfoRepository(passwordInfoDAO)
  lazy val avatarService: AvatarService = new GravatarService(httpLayer)
  lazy val passwordHasherRegistry: PasswordHasherRegistry = new PasswordHasherRegistry(passwordHasher)
  lazy val credentialsProvider: CredentialsProvider = new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  lazy val socialProviderRegistry: SocialProviderRegistry = SocialProviderRegistry(Seq())
  lazy val silhouette: Silhouette[SilhouetteEnv] = new SilhouetteProvider(env, securedAction, unsecuredAction, userAwareAction)

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
}
