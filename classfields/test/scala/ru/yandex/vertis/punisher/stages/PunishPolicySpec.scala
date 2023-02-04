package ru.yandex.vertis.punisher.stages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.Rule.RuleType
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval

import scala.concurrent.duration._

/**
  * @author slider5
  */
@RunWith(classOf[JUnitRunner])
class PunishPolicySpec extends BaseSpec {

  var meteredRunCount = 0

  class RuleNone extends AutoruRule[TaskContext.Batch] {
    override def ruleType: RuleType = RuleType.AutoruOffersClusterTakeQuote
    override def punishment(userCluster: UserCluster[AutoruUser])(
        implicit context: TaskContext.Batch
    ): Option[Punishment] = None
  }

  class RuleTakeQuote extends AutoruRule[TaskContext.Batch] {
    override def ruleType: RuleType = RuleType.AutoruOffersClusterTakeQuote
    override def punishment(userCluster: UserCluster[AutoruUser])(
        implicit context: TaskContext.Batch
    ): Option[Punishment] = Some(AutoruPunishment.TakeQuote(ResellerCategoriesGroup.Auto.toCategories))
  }

  class RuleManualCheck extends AutoruRule[TaskContext.Batch] {
    override def ruleType: RuleType = RuleType.AutoruOffersClusterManualCheck
    override def punishment(userCluster: UserCluster[AutoruUser])(
        implicit context: TaskContext.Batch
    ): Option[Punishment] = Some(AutoruPunishment.ManualCheck(ResellerCategoriesGroup.Auto.toCategories))
  }

  trait MeteredRuleRunCounter extends Rule[AutoruUser] {

    override type Context = TaskContext.Batch

    abstract override def punishment(
        userCluster: UserCluster[AutoruUser]
    )(implicit context: TaskContext.Batch): Option[Punishment] = {
      meteredRunCount += 1
      super.punishment(userCluster)
    }
  }

  private val rulesChain =
    Seq(
      new RuleNone with MeteredRuleRunCounter,
      new RuleNone with MeteredRuleRunCounter,
      new RuleTakeQuote with MeteredRuleRunCounter,
      new RuleManualCheck with MeteredRuleRunCounter,
      new RuleManualCheck with MeteredRuleRunCounter
    )

  private val recentYandexUidsForUser = RecentYandexUidsForUser(yandexUids = Set("xxx"), offers = Set("123-hash"))

  private val emptyUser =
    AutoruUser(
      userId = "5",
      isBanned = Some(true),
      isAutoRuStaff = Some(true),
      offers = Some(Set.empty),
      recentYandexUidsForUser = Some(recentYandexUidsForUser)
    )

  private val cluster = EnrichedUserCluster(Set(emptyUser), "5")

  private val policy = PunishPolicy(rulesChain)

  /**
    * Count number of Rule.apply runs
    * Don't continue if some Rule returns non empty result
    */
  "PunishPoliy" should {
    "verdicts number of operations" in {
      policy.verdict(cluster)(
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
          timeInterval = TimeInterval(DateTimeUtils.now.minusHours(3), 1.hour, None)
        )
      )
      meteredRunCount shouldBe 3
    }
  }
}
