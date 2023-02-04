package ru.yandex.vertis.chat.components.app.config

import com.typesafe.config.{Config, ConfigFactory}
import ru.yandex.vertis.application.Version
import ru.yandex.vertis.application.deploy.Deploys
import ru.yandex.vertis.application.environment.Environments
import ru.yandex.vertis.application.runtime.{RuntimeConfig, RuntimeConfigImpl}
import ru.yandex.vertis.chat._
import ru.yandex.vertis.chat.components.app.ServerConfig
import ru.yandex.vertis.chat.components.clients.broker.NoBrokerConfig
import ru.yandex.vertis.chat.components.clients.bunker.{BunkerConfig, NoBunkerConfig}
import ru.yandex.vertis.chat.components.domains.DomainAware
import ru.yandex.vertis.chat.components.zookeeper.curator.CuratorConfig
import ru.yandex.vertis.chat.config._
import ru.yandex.vertis.chat.util.http.HostPort

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.Random

/**
  * Config for application to be run locally.
  *
  * @author dimas
  */
class LocalAppConfig(locatorConfig: UserLocatorConfig, val domain: Domain) extends AppConfig with DomainAware {

  private val Localhost = "localhost"

  val runtime: RuntimeConfig =
    RuntimeConfigImpl(Environments.Local, "localhost", "local", Deploys.Container, allocation = Some("unit-test"))

  val backend: BackendConfig = BackendConfig(
    Iterable(new SharedJvmServiceConfig {
      def eventServiceConfig: EventServiceConfig = NoEventServiceConfig

      def journalEventServiceConfig(appConfig: AppConfig): JournalEventServiceConfig = NoJournalEventServiceConfig

      def domain: Domain = LocalAppConfig.this.domain

      def userLocatorConfig: UserLocatorConfig = locatorConfig

      def spamServiceConfig: SpamServiceConfig = DumbSpamServiceConfig

      def unreadNotificationDelays: Seq[FiniteDuration] = Seq(1.minute)

      def jivositeConfig: JivositeConfig = NoJivositeConfig

      def aggregatorConfig: CommonAggregatorConfig = NoCommonAggregatorConfig

      def jivositeAggregatorConfig: ExternalAggregatorConfig = NoExternalAggregatorConfig

      def bachataAggregatorConfig: ExternalAggregatorConfig = NoExternalAggregatorConfig

      def passportConfig: PassportConfig = NoPassportConfig

      def bunkerConfig: BunkerConfig = NoBunkerConfig

      def pushnoyConfig: PushnoyConfig = NoPushnoyConfig

      def mdsConfig: MdsConfig = NoMdsConfig

      def s3Config: S3Config = NoS3Config

      def featuresConfig: FeaturesConfig = SimpleFeaturesConfig

      def vertisTechsupportConfig: VertisTechsupportConfig = NoVertisTechsupportConfig

      def techsupportDestinationDeciderConfig: TechsupportDestinationDeciderConfig =
        EmptyTechsupportDestinationDeciderConfig

      override def lanternConfig: BunkerLanternConfig = EmptyBunkerLanternConfig

      override def cleanWebConfig: Option[CleanWebConfig] = None

      override def tvmConfig = None

      override def chatBotDataConfig: Option[ChatBotDataConfig] = None

      override def messagePresetsConfig: Option[MessagePresetsConfig] = None
    }),
    new CuratorConfig {
      def namespace: String = "chat"

      def connectString: String = Localhost
    },
    NoBrokerConfig
  )

  val dynamic: Config = ConfigFactory.empty()

  val server: ServerConfig = new ServerConfig {
    val name: String = "chat-api"

    val apiPort: Int = nextRandomPort()

    val opsPort: Int = nextRandomPort()
  }

  locatorConfig match {
    case static: StaticLocatorConfig =>
      static.register(HostPort(runtime.hostname, server.apiPort))
    case _ =>
  }

  val version: Version = Version.Empty

  private def nextRandomPort(): Int =
    Math.abs(Random.nextInt(10024)) + 1025
}
