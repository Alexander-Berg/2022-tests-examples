package ru.yandex.vertis.punisher.stages.rules.stream

import ru.yandex.vertis.Domain
import ru.yandex.vertis.clustering.proto.Model.ClusteringFormula
import ru.yandex.vertis.generators.NetGenerators.asProducer
import ru.yandex.vertis.moderation.proto.Autoru.AutoruEssentials.Category
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.Generators.AutoruOfferStateGen
import ru.yandex.vertis.punisher.config.OffersCountAutoruConfig.{ManualCheckConfig, ManualCheckTakeQuoteConfig}
import ru.yandex.vertis.punisher.config.{OffersCountAutoruConfig, RulesAutoruConfig}
import ru.yandex.vertis.punisher.config.RulesAutoruConfig.RulesOffersAutoruConfig
import ru.yandex.vertis.punisher.model.{
  AutoruPunishment,
  AutoruUser,
  EnrichedUserCluster,
  ResellerCategoriesGroup,
  TaskContext,
  TaskDomainImpl
}
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.util.DateTimeUtils

class AutoruOffersStreamRulesSpec extends BaseSpec {

  implicit protected val context =
    TaskContext.Stream(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      triggerEventDateTime = DateTimeUtils.now.toInstant
    )
  private val offersCountAutoruConfig =
    OffersCountAutoruConfig(
      active = ManualCheckConfig(1),
      perThreeMonth = ManualCheckConfig(2),
      perHalfYear = ManualCheckTakeQuoteConfig(5, 3),
      perYear = ManualCheckTakeQuoteConfig(9, 5)
    )
  private val rulesOffersAutoruConfig = RulesOffersAutoruConfig(1, 2, offersCountAutoruConfig)
  private val rulesAutoruConfig = RulesAutoruConfig(rulesOffersAutoruConfig)
  private val ruleClusterTakeQuote = new AutoruOffersStreamRules.ClusterTakeQuote(rulesAutoruConfig)
  private val ruleClusterActiveManualCheck = new AutoruOffersStreamRules.ClusterActiveManualCheck(rulesAutoruConfig)

  private val thirtyDaysAgo = DateTimeUtils.now.minusDays(30)
  private val yesterday = DateTimeUtils.now.minusDays(1)
  private val userId1 = "userId1"
  private val userId2 = "userId2"
  private val userId3 = "userId3"

  "AutoruOffersStreamRules.ClusterTakeQuote" should {

    "punish in ResellerCategoriesGroup.Moto categories for 7 offers for all cluster in last half of year" in {
      val offerStates1 =
        AutoruOfferStateGen
          .next(2)
          .toSeq
          .map(
            _.copy(
              userId = userId1,
              category = Some(Category.MOTORCYCLE),
              isActive = true
            )
          )
      val user1 =
        AutoruUser(
          userId = userId1,
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen
          .next(2)
          .toSeq
          .map(
            _.copy(
              userId = userId2,
              category = Some(Category.SNOWMOBILE),
              isActive = true
            )
          )
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates3 =
        AutoruOfferStateGen
          .next(3)
          .toSeq
          .map(
            _.copy(
              userId = userId3,
              category = Some(Category.SCOOTERS),
              isActive = true
            )
          )
      val user3 =
        AutoruUser(
          userId = userId3,
          offersStates = Some(offerStates3),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2, user3), userId1, Some(ClusteringFormula.L1_STRICT))
      val expectedResult = Some(AutoruPunishment.TakeQuote(ResellerCategoriesGroup.Moto.toCategories))

      ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe expectedResult
    }

    "not punish for 3 cars and 4 moto for all cluster in last half of year" in {
      val offerStates1 =
        AutoruOfferStateGen.next(3).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
      val user1 =
        AutoruUser(
          userId = userId1,
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen
          .next(4)
          .toSeq
          .map(_.copy(userId = userId2, category = Some(Category.MOTORCYCLE), isActive = true))
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))

      ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe None
    }

    "not punish for 7 cars for all cluster in last half of year if cluster leader is banned already" in {
      val offerStates1 =
        AutoruOfferStateGen.next(3).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
      val user1 =
        AutoruUser(
          userId = userId1,
          isBanned = Some(true),
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen.next(4).toSeq.map(_.copy(userId = userId2, category = Some(Category.CARS), isActive = true))
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))

      ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe None
    }

    "not punish for 7 cars for all cluster in last half of year if private member of cluster is banned" in {
      val offerStates1 =
        AutoruOfferStateGen.next(3).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
      val user1 =
        AutoruUser(
          userId = userId1,
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen
          .next(4)
          .toSeq
          .map(_.copy(userId = userId2, category = Some(Category.CARS), isActive = true))
      val user2 =
        AutoruUser(
          userId = userId2,
          isBanned = Some(true),
          isAutoRuStaff = Some(false),
          isClient = Some(false),
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))

      ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe None
    }

    "punish in CARS category if leader is already banned in LCV" in {
      val offerStates1 =
        AutoruOfferStateGen.next(3).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
      val user1 =
        AutoruUser(
          userId = userId1,
          resellerCategories = Some(Set(UsersAutoru.LCV)),
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen.next(4).toSeq.map(_.copy(userId = userId2, category = Some(Category.LCV), isActive = true))
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))
      val expectedResult = Some(AutoruPunishment.TakeQuote(Set(UsersAutoru.CARS)))

      ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe expectedResult
    }
  }

  "punish in CARS category if leader has quota returned in LCV" in {
    val offerStates1 =
      AutoruOfferStateGen.next(3).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
    val user1 =
      AutoruUser(
        userId = userId1,
        resellerUpdatedByCategories = Some(Map(UsersAutoru.LCV -> thirtyDaysAgo.minusDays(60))),
        offersStates = Some(offerStates1),
        lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
      )
    val offerStates2 =
      AutoruOfferStateGen.next(4).toSeq.map(_.copy(userId = userId2, category = Some(Category.LCV), isActive = true))
    val user2 =
      AutoruUser(
        userId = userId2,
        offersStates = Some(offerStates2),
        lastYandexUidDateTime = Some(thirtyDaysAgo.toInstant)
      )
    val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))
    val expectedResult = Some(AutoruPunishment.TakeQuote(Set(UsersAutoru.CARS)))

    ruleClusterTakeQuote.punishment(userCluster)(context) shouldBe expectedResult
  }

  "AutoruOffersStreamRules.ClusterActiveManualCheck" should {

    "punish in ResellerCategoriesGroup.Moto categories for 3 offers for all cluster in 2 days" in {
      val offerStates1 =
        AutoruOfferStateGen
          .next(2)
          .toSeq
          .map(
            _.copy(
              userId = userId1,
              category = Some(Category.MOTORCYCLE),
              isActive = true,
              placedForFree = Some(true)
            )
          )
      val user1 =
        AutoruUser(
          userId = userId1,
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(yesterday.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen
          .next(2)
          .toSeq
          .map(
            _.copy(
              userId = userId2,
              category = Some(Category.SNOWMOBILE),
              isActive = true,
              placedForFree = Some(true)
            )
          )
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(yesterday.toInstant)
        )
      val offerStates3 =
        AutoruOfferStateGen
          .next(3)
          .toSeq
          .map(
            _.copy(
              userId = userId3,
              category = Some(Category.SCOOTERS),
              isActive = true,
              placedForFree = Some(true)
            )
          )
      val user3 =
        AutoruUser(
          userId = userId3,
          offersStates = Some(offerStates3),
          lastYandexUidDateTime = Some(yesterday.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2, user3), userId1, Some(ClusteringFormula.L1_STRICT))
      val expectedResult = Some(AutoruPunishment.ManualCheck(ResellerCategoriesGroup.Moto.toCategories))

      ruleClusterActiveManualCheck.punishment(userCluster)(context) shouldBe expectedResult
    }

    "not punish for 2 offers for all cluster in 2 days if one is placed not for free" in {
      val offerStates1 =
        AutoruOfferStateGen.next(1).toSeq.map(_.copy(userId = userId1, category = Some(Category.CARS), isActive = true))
      val user1 =
        AutoruUser(
          userId = userId1,
          offersStates = Some(offerStates1),
          lastYandexUidDateTime = Some(yesterday.toInstant)
        )
      val offerStates2 =
        AutoruOfferStateGen
          .next(1)
          .toSeq
          .map(
            _.copy(
              userId = userId2,
              category = Some(Category.LCV),
              isActive = true,
              placedForFree = Some(false)
            )
          )
      val user2 =
        AutoruUser(
          userId = userId2,
          offersStates = Some(offerStates2),
          lastYandexUidDateTime = Some(yesterday.toInstant)
        )
      val userCluster = EnrichedUserCluster(Set(user1, user2), userId1, Some(ClusteringFormula.L1_STRICT))

      ruleClusterActiveManualCheck.punishment(userCluster)(context) shouldBe None
    }
  }
}
