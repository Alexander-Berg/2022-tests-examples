package ru.yandex.vertis.punisher

import ru.yandex.vertis.punisher.config.OffersCountAutoruConfig.{ManualCheckConfig, ManualCheckTakeQuoteConfig}
import ru.yandex.vertis.punisher.config.RulesAutoruConfig.RulesOffersAutoruConfig
import ru.yandex.vertis.punisher.config.{
  MysqlConfig,
  OffersCountAutoruConfig,
  RulesAutoruConfig,
  VertisPassportApiConfig
}
import ru.yandex.vertis.punisher.database.{DatabaseFactory, DatabaseReadOnly}
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.services.ModerationService
import ru.yandex.vertis.punisher.stages._
import ru.yandex.vertis.punisher.stages.impl._

import java.net.URL
import scala.concurrent.duration._

/**
  * @author devreggs
  */
object AutoruStagesBuilder extends BaseSpec {

  val vertisPassportConfig: VertisPassportApiConfig =
    VertisPassportApiConfig(
      domain = "auto",
      queueSize = 500000,
      throttleMaxBurst = 5,
      url = new URL("http://passport-api-01-sas.test.vertis.yandex.net:6210"),
      throttleRps = 30,
      retryCount = 3,
      retryDelay = 10.seconds
    )

  val clusterizer: Clusterizer[F] = MockAutoruServicesBuilder.clusterizer

  val emptyClusterizer: Clusterizer[F] = new EmptyClusterizerImpl[F]

  val moderationService: ModerationService[F] = MockAutoruServicesBuilder.moderationService

  lazy val finder: FinderImpl[F] = new FinderImpl(MockAutoruDaoBuilder.activityDao)

  val baseEnricher: Enricher[F, AutoruUser, TaskContext.Batch] = new AutoruBaseEnricher(moderationService)

  val autoruOffersRulesConfig: RulesAutoruConfig =
    RulesAutoruConfig(
      offers =
        RulesOffersAutoruConfig(
          yandexUidIncrease = 1,
          minLimit = 2,
          clusterOffersCount =
            OffersCountAutoruConfig(
              active = ManualCheckConfig(1),
              perThreeMonth = ManualCheckConfig(2),
              perHalfYear = ManualCheckTakeQuoteConfig(takeQuote = 5, manualCheck = 3),
              perYear = ManualCheckTakeQuoteConfig(takeQuote = 9, manualCheck = 5)
            )
        )
    )
  private val offersRulesChain: Seq[AutoruRule[TaskContext.Batch]] = Seq()

  private val cbirRulesChain: Seq[AutoruRule[TaskContext.Batch]] =
    Seq(
      new AutoruCbirRules.ManualCheckRule
    )

  private val resellerAmnestyRulesChain: Seq[AutoruRule[TaskContext.Batch]] =
    Seq(
      new AutoruResellerAmnestyRules.ClusterOffersReturnQuota
    )

  val offersPunishPolicy: PunishPolicy[AutoruUser, TaskContext.Batch] = PunishPolicy(offersRulesChain)

  val cbirPunishPolicy: PunishPolicy[AutoruUser, TaskContext.Batch] = PunishPolicy(cbirRulesChain)

  val resellerAmnestyPunishPolicy: PunishPolicy[AutoruUser, TaskContext.Batch] = PunishPolicy(resellerAmnestyRulesChain)

  val punisher: Punisher[F, AutoruUser] = new EmptyPunisher[F, AutoruUser]

  private lazy val mainDbConfig: MysqlConfig =
    MysqlConfig(
      driver = "com.mysql.jdbc.Driver",
      readUrl = "jdbc:mysql://mysql.dev.vertis.yandex.net:3305/users",
      writeUrl = Some("jdbc:mysql://mysql.dev.vertis.yandex.net:3305/users"),
      username = "user-punisher",
      password = "12345678",
      idleTimeout = 1.minute,
      maxConnections = 2,
      executorNumThreads = 2,
      executorQueueSize = 1000,
      writeValidationQuery = None,
      readValidationQuery = None
    )

  lazy val dbMain: DatabaseReadOnly = DatabaseFactory.getOnlyReadDatabase(mainDbConfig)
}
