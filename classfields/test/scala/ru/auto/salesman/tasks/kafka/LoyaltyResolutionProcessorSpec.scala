package ru.auto.salesman.tasks.kafka

import org.joda.time.DateTime
import ru.auto.salesman.model.{ClientId, PeriodId, RegionId}
import ru.auto.salesman.model.cashback.{CashbackPeriod, ExclusivityParams, LoyaltyLevel}
import ru.auto.salesman.model.cashback.ApiModel.LoyaltyReport
import ru.auto.salesman.model.cashback.LoyaltyLevel.YearLoyaltyLevel
import ru.auto.salesman.service.{
  LoyaltyParamsFetcher,
  LoyaltyReportService,
  RewardService
}
import ru.auto.salesman.service.RewardService.{Cashback, CashbackPercent, HasRewardParams}
import ru.auto.salesman.service.LoyaltyParamsFetcher.{
  LoyaltyParams,
  LoyaltyRewardParams,
  PlacementDiscountResult
}
import ru.auto.salesman.tasks.kafka.processors.impl.LoyaltyResolutionProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.yandex.vertis.moderation.proto.Model._
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata.LoyaltyInfo
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata.LoyaltyInfo.Loyal

class LoyaltyResolutionProcessorSpec extends BaseSpec {
  private val loyaltyReportService = mock[LoyaltyReportService]
  private val loyaltyParamsFetcher = mock[LoyaltyParamsFetcher]
  val rewardService = mock[RewardService]

  private val processor = new LoyaltyResolutionProcessorImpl(
    loyaltyReportService,
    loyaltyParamsFetcher,
    rewardService
  )

  private val report = {
    val loyaltyInfo = LoyaltyInfo
      .newBuilder()
      .setLoyal(Loyal.newBuilder().setLevel(12))
      .setPeriodId("1")

    val opinions = Opinions.newBuilder().setVersion(3)

    val dealerMetadata = DealerMetadata
      .newBuilder()
      .setForPeriodApproved(loyaltyInfo)

    val metadata = Metadata
      .newBuilder()
      .setDealerMetadata(dealerMetadata)

    val user = User.newBuilder().setDealerUser("1").setVersion(3)
    val externalId = ExternalId.newBuilder().setUser(user).setVersion(3)

    val instance = Instance
      .newBuilder()
      .addMetadata(metadata)
      .setOpinions(opinions)
      .setExternalId(externalId)
      .setVersion(3)
      .setHashVersion(3)
      .build()

    UpdateJournalRecord
      .newBuilder()
      .setInstance(instance)
      .setVersion(3)
      .build()
  }

  "LoyaltyResolutionProcessor" should {
    "upsert report with items" in {
      val periodId = PeriodId(1)
      val clientId = 1L
      val now = new DateTime()

      val loyaltyParams = LoyaltyParams(
        extrabonus = None,
        hasFullStock = true,
        period = CashbackPeriod(
          id = periodId,
          start = now,
          finish = now,
          isActive = true,
          previousPeriod = None
        ),
        regionId = RegionId(1),
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )

      val cashback = Cashback(
        percent = CashbackPercent(
          percent = 10,
          hasFullStock = true,
          extraBonus = None,
          proportions = None
        ),
        amount = 10,
        activationsAmount = 100.0,
        exclusivityParams = ExclusivityParams(resolution = true, 70, 60)
      )

      val upsertReport = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(true)
        .setCashbackAmount(10L)
        .setCashbackPercent(10)
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.YearLoyaltyLevel.raw)
        .setActivationsAmount(100L)
        .setHasFullStock(true)
        .setAutoruExclusivePercent(70)
        .setPlacementDiscountPercent(5)
        .build()

      report.clientId shouldBe 1L
      report.periodId shouldBe Some(1L)
      report.loyaltyLevel shouldBe Some(12)
      report.resolution shouldBe Some(LoyaltyInfo.ResolutionCase.LOYAL)

      (loyaltyParamsFetcher.fetchPeriod _)
        .expects(periodId)
        .returningZ(loyaltyParams.period)

      (loyaltyParamsFetcher.getParams _)
        .expects(periodId, clientId)
        .returningZ(loyaltyParams)

      (loyaltyReportService.findActivationsAmount _)
        .expects(clientId, periodId)
        .returningZ(None)

      (rewardService
        .resolveCashback(
          _: ClientId,
          _: LoyaltyLevel,
          _: PeriodId,
          _: LoyaltyParams,
          _: Option[LoyaltyInfo]
        ))
        .expects(clientId, YearLoyaltyLevel, periodId, loyaltyParams, *)
        .returningZ(cashback)

      (rewardService
        .isPlacementDiscountAvailable(
          _: LoyaltyLevel,
          _: HasRewardParams
        ))
        .expects(YearLoyaltyLevel, LoyaltyRewardParams(loyaltyParams))
        .returningZ(true)

      (loyaltyReportService.upsert _)
        .expects(upsertReport, *)
        .returningZ(unit)

      processor.process(report).success.value
    }

    "upsert report with already fetched activations count" in {
      val periodId = PeriodId(1)
      val clientId = 1L
      val now = new DateTime()

      val loyaltyParams = LoyaltyParams(
        extrabonus = None,
        hasFullStock = true,
        period = CashbackPeriod(
          id = periodId,
          start = now,
          finish = now,
          isActive = true,
          previousPeriod = None
        ),
        regionId = RegionId(1),
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(5)
      )

      val cashback = Cashback(
        percent = CashbackPercent(
          percent = 10,
          hasFullStock = true,
          extraBonus = None,
          proportions = None
        ),
        amount = 10,
        activationsAmount = 100.0,
        exclusivityParams = ExclusivityParams(resolution = true, 70, 60)
      )

      val upsertReport = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(true)
        .setCashbackAmount(10L)
        .setCashbackPercent(10)
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.YearLoyaltyLevel.raw)
        .setActivationsAmount(100L)
        .setHasFullStock(true)
        .setAutoruExclusivePercent(70)
        .setPlacementDiscountPercent(5)
        .build()

      report.clientId shouldBe 1L
      report.periodId shouldBe Some(1L)
      report.loyaltyLevel shouldBe Some(12)
      report.resolution shouldBe Some(LoyaltyInfo.ResolutionCase.LOYAL)

      (loyaltyParamsFetcher.fetchPeriod _)
        .expects(periodId)
        .returningZ(loyaltyParams.period)

      (loyaltyParamsFetcher.getParams _)
        .expects(periodId, clientId)
        .returningZ(loyaltyParams)

      (loyaltyReportService.findActivationsAmount _)
        .expects(clientId, periodId)
        .returningZ(Some(123))

      (rewardService.calculateCashbackByInfo _)
        .expects(loyaltyParams, YearLoyaltyLevel, *, 123)
        .returningZ(cashback)

      (rewardService
        .isPlacementDiscountAvailable(
          _: LoyaltyLevel,
          _: HasRewardParams
        ))
        .expects(YearLoyaltyLevel, LoyaltyRewardParams(loyaltyParams))
        .returningZ(true)

      (loyaltyReportService.upsert _)
        .expects(upsertReport, *)
        .returningZ(unit)

      processor.process(report).success.value
    }
  }
}
