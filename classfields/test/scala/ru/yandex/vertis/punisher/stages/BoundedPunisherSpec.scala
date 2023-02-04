package ru.yandex.vertis.punisher.stages

import cats.effect.Sync
import cats.syntax.applicative._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.punisher.BaseSpec
import ru.yandex.vertis.punisher.model.Rule.RuleType
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.stages.impl.BoundedPunisher
import ru.yandex.vertis.punisher.stages.impl.BoundedPunisher.OverPunishedResult
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.punisher.util.{DateTimeUtils, ZookeeperUtils}

import scala.concurrent.duration.Duration
import scala.language.reflectiveCalls

@RunWith(classOf[JUnitRunner])
class BoundedPunisherSpec extends BaseSpec {

  private val sync: Sync[F] = Sync[F]

  private val punisher =
    new Punisher[F, AutoruUser] with BoundedPunisher[F, AutoruUser] {

      override protected def zkUtils: ZookeeperUtils = ???

      override protected def taskDomain: TaskDomain = ???

      override protected def maxByPunishment(punishment: Punishment): Option[Int] =
        punishment match {
          case AutoruPunishment.TakeQuote(_) => Some(2)
          case _                             => None
        }

      implicit override protected def sync: Sync[F] = BoundedPunisherSpec.this.sync

      override protected def maxTotal: Int = 3

      override def doPunish(verdict: Verdict[AutoruUser], rulePunishment: RulePunishment[AutoruUser])(
          implicit context: TaskContext
      ): F[Unit] = ().pure

      def testOverPunished(verdicts: Set[Verdict[AutoruUser]])(
          implicit context: TaskContext
      ): Option[OverPunishedResult] = overPunished(verdicts)
    }

  implicit private val context: TaskContext.Batch =
    TaskContext.Batch(
      taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.Offers),
      timeInterval = TimeInterval(DateTimeUtils.now, Duration.fromNanos(1000L))
    )

  class RuleTakeQuote extends AutoruRule[TaskContext.Batch] {
    override def ruleType: RuleType = RuleType.AutoruOffersClusterTakeQuote

    override def punishment(userCluster: UserCluster[AutoruUser])(
        implicit context: TaskContext.Batch
    ): Option[Punishment] = Some(AutoruPunishment.TakeQuote(ResellerCategoriesGroup.Auto.toCategories))
  }

  private val rule = new RuleTakeQuote

  private val punishmentTakeQuote = AutoruPunishment.TakeQuote(ResellerCategoriesGroup.Auto.toCategories)

  private val none =
    Set(
      Verdict[AutoruUser](EnrichedUserCluster[AutoruUser](Set.empty, "1"), None),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "2"),
        Some(RulePunishment(rule, punishmentTakeQuote))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "3"),
        Some(RulePunishment(rule, punishmentTakeQuote))
      )
    )
  private val total =
    Set(
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "1"),
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "2"),
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "3"),
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "4"),
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      )
    )
  private val ban =
    Set(
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "1"),
        Some(RulePunishment(rule, punishmentTakeQuote))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "2"),
        Some(RulePunishment(rule, punishmentTakeQuote))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "3"),
        Some(RulePunishment(rule, punishmentTakeQuote))
      ),
      Verdict[AutoruUser](
        EnrichedUserCluster[AutoruUser](Set.empty, "4"),
        Some(RulePunishment(rule, AutoruPunishment.Ban(Reason.DO_NOT_EXIST, None)))
      )
    )

  "BoundedPunisher" should {
    "none" in {
      punisher.testOverPunished(none).isEmpty shouldBe true
    }
    "total" in {
      punisher.testOverPunished(total).isDefined shouldBe true
    }
    "ban" in {
      val r = punisher.testOverPunished(ban)
      r.isDefined shouldBe true
      r.get.overPunished.nonEmpty shouldBe true
    }
  }
}
