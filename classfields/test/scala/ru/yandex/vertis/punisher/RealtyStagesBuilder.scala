package ru.yandex.vertis.punisher

import ru.yandex.vertis.feature.impl.{BasicFeatureTypes, InMemoryFeatureRegistry}
import ru.yandex.vertis.punisher.config.OffersCountRealtyConfig.PerHalfYearRealtyConfig
import ru.yandex.vertis.punisher.config.RulesRealtyConfig.RulesOffersRealtyConfig
import ru.yandex.vertis.punisher.config.{OffersCountRealtyConfig, RulesRealtyConfig}
import ru.yandex.vertis.punisher.model.RuleTree.Implicits._
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.stages._
import ru.yandex.vertis.punisher.stages.impl._
import ru.yandex.vertis.quality.feature_registry_utils.FeatureRegistryF

object RealtyStagesBuilder extends BaseSpec {

  val clusterizer: Clusterizer[F] = MockRealtyServicesBuilder.clusterizer

  val offersFinder: Finder[F] = new FinderImpl(MockRealtyDaoBuilder.offersActivityDao)

  val infectionFinder: Finder[F] = new FinderImpl(MockRealtyDaoBuilder.infectionActivityDao)

  val offersEnricher: Enricher[F, RealtyUser, TaskContext.Batch] =
    new RealtyOffersEnricher(MockRealtyServicesBuilder.moderationService, MockRealtyDaoBuilder.offersDao)

  val baseEnricher: Enricher[F, RealtyUser, TaskContext.Batch] =
    new RealtyBaseEnricher(MockRealtyServicesBuilder.moderationService)

  val realtyOffersRulesConfig: RulesRealtyConfig =
    RulesRealtyConfig(
      offers =
        RulesOffersRealtyConfig(
          userOffersCount = OffersCountRealtyConfig(perHalfYear = PerHalfYearRealtyConfig(markAsAgent = 3)),
          clusterOffersCount = OffersCountRealtyConfig(perHalfYear = PerHalfYearRealtyConfig(markAsAgent = 3))
        )
    )

  val featureRegistry: FeatureRegistryF[F] = new FeatureRegistryF[F](new InMemoryFeatureRegistry(BasicFeatureTypes))

  private val offersRuleTree: RuleTree[RealtyUser, TaskContext.Batch] =
    new RealtyOffersRules.UserMarkAsAgent(realtyOffersRulesConfig).tree ~>
      new RealtyOffersRules.ClusterMarkAsAgent(realtyOffersRulesConfig).tree |
      new RealtyOffersRules.ClusterTakeQuota(featureRegistry).tree

  private val infectionRuleTree: RuleTree[RealtyUser, TaskContext.Batch] =
    new RealtyInfectionRules.ClusterMarkAsAgent().tree ~>
      new RealtyInfectionRules.ClusterBan().tree |
      new RealtyInfectionRules.ClusterTakeQuota().tree

  val offersPunishPolicy: PunishPolicy[RealtyUser, TaskContext.Batch] = PunishPolicy(offersRuleTree)

  val infectionPunishPolicy: PunishPolicy[RealtyUser, TaskContext.Batch] = PunishPolicy(infectionRuleTree)

  val punisher: Punisher[F, RealtyUser] = new EmptyPunisher[F, RealtyUser]
}
