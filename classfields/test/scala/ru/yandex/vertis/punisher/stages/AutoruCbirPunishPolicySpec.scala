package ru.yandex.vertis.punisher.stages

import cats.syntax.applicative._
import ru.yandex.vertis.quality.cats_utils.Awaitable._
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.Domain
import ru.yandex.vertis.punisher.dao.OfferSimilarityDao
import ru.yandex.vertis.punisher.dao.OfferSimilarityDao.{Offer, OfferSimilarity}
import ru.yandex.vertis.punisher.model.AutoruPunishment.ManualCheck
import ru.yandex.vertis.punisher.model.TaskDomain.Labels
import ru.yandex.vertis.punisher.model._
import ru.yandex.vertis.punisher.stages.impl._
import ru.yandex.vertis.punisher.tasks.settings.TaskSettings
import ru.yandex.vertis.punisher.util.DateTimeUtils
import ru.yandex.vertis.punisher.util.DateTimeUtils.TimeInterval
import ru.yandex.vertis.punisher.{AutoruStagesBuilder, BaseSpec}

import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class AutoruCbirPunishPolicySpec extends BaseSpec {

  private val taskSettings: TaskSettings = TaskSettings(1.hours, 1.hours, 24.hours)

  private val offerSimilarityDao: OfferSimilarityDao[F] =
    (_: TaskContext) =>
      Iterable(
        // Ordinary user - punishable
        OfferSimilarity(
          offer =
            Offer(
              offerId = "offer1",
              userId = "200",
              category = OfferSimilarityDao.HolocronCategory.Auto,
              bodyType = "SEDAN"
            ),
          similarOffers =
            Set(
              Offer(
                offerId = "offer2",
                userId = "201",
                category = OfferSimilarityDao.HolocronCategory.Auto,
                bodyType = "SEDAN"
              )
            )
        ),
        // Reseller only in CARS category (without LCV) - punishable
        OfferSimilarity(
          offer =
            Offer(
              offerId = "offer3",
              userId = "201",
              category = OfferSimilarityDao.HolocronCategory.Trucks,
              bodyType = "LCV"
            ),
          similarOffers =
            Set(
              Offer(
                offerId = "offer4",
                userId = "202",
                category = OfferSimilarityDao.HolocronCategory.Trucks,
                bodyType = "LCV"
              ),
              Offer(
                offerId = "offer5",
                userId = "202",
                category = OfferSimilarityDao.HolocronCategory.Trucks,
                bodyType = "LCV"
              )
            )
        ),
        // Already reseller in CARS & LCV - is not punishable
        OfferSimilarity(
          offer =
            Offer(
              offerId = "offer6",
              userId = "202",
              category = OfferSimilarityDao.HolocronCategory.Auto,
              bodyType = "SEDAN"
            ),
          similarOffers =
            Set(
              Offer(
                offerId = "offer6",
                userId = "202",
                category = OfferSimilarityDao.HolocronCategory.Auto,
                bodyType = "SEDAN"
              )
            )
        )
      ).pure[F]

  private val enricher: Enricher[F, AutoruUser, TaskContext.Batch] =
    new AutoruSimilarOffersEnricher(
      offerSimilarityDao,
      AutoruStagesBuilder.moderationService
    )

  private val policy: PunishPolicy[AutoruUser, TaskContext.Batch] = AutoruStagesBuilder.cbirPunishPolicy

  "AutoruCbirPunishPolicy" should {
    "Rules" in {
      implicit val context: TaskContext.Batch =
        TaskContext.Batch(
          taskDomain = TaskDomainImpl(Domain.DOMAIN_AUTO, Labels.CbirYtCars),
          timeInterval = TimeInterval(DateTimeUtils.now, taskSettings.stepMin, Some(taskSettings.stepBack))
        )
      val cbirOfferSimilarities = offerSimilarityDao.get(context).await
      val clusters =
        cbirOfferSimilarities.map { i =>
          val userId = i.offer.userId
          UserIdCluster(members = Set(userId), leader = userId)
        }.toSet
      val epr = AutoruEnrichPrepareResult(clusters = clusters, cbirOfferSimilarities = cbirOfferSimilarities)
      val enriched = epr.clusters.map(enricher.enrich(_, Some(epr)).await)
      val verdicts = enriched.flatMap(policy.verdict).filter(_.rulePunishment.nonEmpty)

      val actual =
        verdicts.map { v =>
          (v.cluster.leader.userId, v.rulePunishment.get.punishment)
        }

      actual should contain theSameElementsAs Set(
        ("200", ManualCheck(ResellerCategoriesGroup.Auto.toCategories)),
        ("201", ManualCheck(ResellerCategoriesGroup.Auto.toCategories))
      )
    }
  }
}
