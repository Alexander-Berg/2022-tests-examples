package ru.yandex.vertis.punisher.stages

import cats.syntax.applicative._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.moderation.proto.Model.Domain.UsersAutoru
import ru.yandex.vertis.punisher.dao.{ResellersDao, UsersActivityDao}
import ru.yandex.vertis.punisher.model.AutoruPunishment.ReturnQuote
import ru.yandex.vertis.punisher.model.ResellerCategoriesGroup.Label
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.stages.impl.{AutoruResellerAmnestyEnricher, AutoruResellerAmnestyFinder}
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.punisher.{AutoruStagesBuilder, BaseSpec}
import ru.yandex.vertis.quality.cats_utils.Awaitable._

import scala.concurrent.duration._

/**
  * @author devreggs
  */
@RunWith(classOf[JUnitRunner])
class AutoruResellerAmnestyPunishPolicySpec extends BaseSpec {

  private val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 24.hours)

  private val resellersDao =
    new ResellersDao[F] {
      override def resellers(implicit context: TaskContext): F[Map[UserId, Set[UsersAutoru]]] =
        Map(
          "5" -> Set(UsersAutoru.BUS, UsersAutoru.CARS, UsersAutoru.MOTORCYCLE),
          "60" -> Set(UsersAutoru.LCV),
          "700" -> Set(UsersAutoru.MOTORCYCLE)
        ).pure[F]
    }

  private val usersActivityDao =
    new UsersActivityDao[F] {
      override def clusterOffersActivity(userIds: Set[UserId], timeInterval: DateTimeUtils.TimeInterval)(
          implicit context: TaskContext
      ): F[Map[UserId, Set[Label]]] =
        Map(
          "5" -> Set(ResellerCategoriesGroup.Moto, ResellerCategoriesGroup.Commercial),
          "60" -> Set(ResellerCategoriesGroup.Auto, ResellerCategoriesGroup.Commercial)
        ).pure[F]
    }

  private val finder = new AutoruResellerAmnestyFinder(resellersDao)

  private def clusterize(userId: UserId): UserIdCluster = UserIdCluster(Set(userId), userId)

  private val enricher: Enricher[F, AutoruUser, TaskContext.Batch] =
    new AutoruResellerAmnestyEnricher(
      resellersDao,
      usersActivityDao,
      AutoruStagesBuilder.moderationService
    )

  private val policy: PunishPolicy[AutoruUser, TaskContext.Batch] = AutoruStagesBuilder.resellerAmnestyPunishPolicy

  "AutoruResellerAmnestyPunishPolicy" should {
    "triggers rules" in {

      implicit val context: TaskContext.Batch =
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.ResellerAmnesty),
          timeInterval = TimeInterval(DateTimeUtils.now, taskSettings.stepMin, Some(taskSettings.stepBack))
        )
      val actives = finder.find.await.map(clusterize)

      val prepare = enricher.prepare(actives).await

      val enriched = prepare.toSeq.flatMap(_.clusters).map(enricher.enrich(_, prepare).await)

      val verdicts = enriched.flatMap(policy.verdict).filter(_.rulePunishment.nonEmpty)

      val actual =
        verdicts.map { v =>
          (v.cluster.leader.userId, v.rulePunishment.get.punishment)
        }

      actual should contain theSameElementsAs Set(
        ("5", ReturnQuote(Set(UsersAutoru.CARS, UsersAutoru.LCV))),
        ("700", ReturnQuote(Set(UsersAutoru.MOTORCYCLE, UsersAutoru.SCOOTERS, UsersAutoru.SNOWMOBILE, UsersAutoru.ATV)))
      )
    }
  }

}
