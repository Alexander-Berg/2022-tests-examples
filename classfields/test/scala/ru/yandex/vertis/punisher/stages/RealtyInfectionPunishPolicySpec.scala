package ru.yandex.vertis.punisher.stages

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.moderation.proto.Model.Reason
import ru.yandex.vertis.punisher._
import ru.yandex.vertis.punisher.model.RealtyPunishment.{Ban, MarkAsAgent}
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model.{RealtyUser, TaskContext, TaskDomainImpl}
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class RealtyInfectionPunishPolicySpec extends BaseSpec {

  private val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 24.hours)

  private val finder: Finder[F] = RealtyStagesBuilder.infectionFinder

  private val clusterizer: Clusterizer[F] = RealtyStagesBuilder.clusterizer

  private val enricher: Enricher[F, RealtyUser, TaskContext.Batch] = RealtyStagesBuilder.offersEnricher

  private val policy: PunishPolicy[RealtyUser, TaskContext.Batch] = RealtyStagesBuilder.infectionPunishPolicy

  "RealtyInfectionPunishPolicySpec" should {
    "Rules" in {
      implicit val context: TaskContext.Batch =
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_REALTY, Labels.Offers),
          timeInterval = TimeInterval(DateTimeUtils.now, taskSettings.stepMin, Some(taskSettings.stepBack))
        )
      val userIds = finder.find(context).await
      val clusters = userIds.map(clusterizer.clusterize(_).await)
      val epr =
        MockRealtyDaoBuilder.offersDao
          .offers(userIds)
          .map(offers => RealtyEnrichPrepareResult(clusters = clusters, offers = offers))
          .await
      val enriched = clusters.map(enricher.enrich(_, Some(epr)).await)
      val verdicts = enriched.flatMap(policy.verdict).filter(_.rulePunishment.nonEmpty)

      verdicts
        .map(v => (v.cluster.leader.userId, v.rulePunishment.get.punishment)) should contain theSameElementsAs Set(
        ("1", Ban(Reason.USER_FRAUD, None)),
        ("4", MarkAsAgent)
      )
    }
  }
}
