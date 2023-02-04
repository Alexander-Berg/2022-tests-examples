package ru.auto.salesman.tasks.kafka

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.cabinet.ApiModel.ExtraBonus
import ru.auto.salesman.dao.LoyaltyReportDao.ActiveNotApprovedReportInfo
import ru.auto.salesman.model.cashback.CriteriaUpdates.LoyaltyCriteriaUpdates
import ru.auto.salesman.model.cashback._
import ru.auto.salesman.model.{PeriodId, RegionId}
import ru.auto.salesman.service.LoyaltyParamsFetcher.LoyaltyRewardParams
import ru.auto.salesman.service.RewardService.{Cashback, CashbackParams, CashbackPercent}
import ru.auto.salesman.service.LoyaltyReportService.LoyaltyReportActualization
import ru.auto.salesman.service.{
  LoyaltyParamsFetcher,
  LoyaltyReportService,
  RewardService
}
import ru.auto.salesman.tasks.kafka.processors.impl.LoyaltyActualizationProcessorImpl
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.feature.TestDealerFeatureService

class LoyaltyActualizationProcessorSpec extends BaseSpec {

  private val loyaltyReportService = mock[LoyaltyReportService]
  private val loyaltyParamsFetcher = mock[LoyaltyParamsFetcher]
  private val rewardService = mock[RewardService]

  private val featureService =
    TestDealerFeatureService(loyaltyParamsActualizationEnabled = true)

  private val processor = new LoyaltyActualizationProcessorImpl(
    loyaltyParamsFetcher,
    loyaltyReportService,
    rewardService,
    featureService
  )

  "LoyaltyParamsActualizationTask" should {
    "actualize report params" in {
      val clientId = 1L

      val update = LoyaltyCriteriaUpdates
        .newBuilder()
        .setClientId(clientId)
        .setExtraBonus(ExtraBonus.OVER_2000_CARS)
        .setHasFullStock(true)
        .setPlacementDiscount(25)
        .build()

      (loyaltyReportService.findActiveNotApproved _)
        .expects(clientId)
        .returningZ(
          List(
            ActiveNotApprovedReportInfo(
              id = 1,
              clientId = 1,
              periodId = PeriodId(1),
              loyaltyLevel = LoyaltyLevel.HalfYearLoyaltyLevel,
              exclusivityPercent = 10,
              activations = 100
            )
          )
        )

      val regionId = RegionId(1)
      (loyaltyParamsFetcher.fetchClientRegion _)
        .expects(clientId)
        .returningZ(regionId)

      val params =
        CashbackParams(
          extrabonus = Some(ExtraBonus.OVER_2000_CARS),
          hasFullStock = true,
          regionId = regionId
        )

      (rewardService.calculateCashback _)
        .expects(params, LoyaltyLevel.HalfYearLoyaltyLevel, 10, 100)
        .returningZ(
          Cashback(
            percent = CashbackPercent(
              percent = 10,
              hasFullStock = true,
              extraBonus = Some(ExtraBonus.OVER_2000_CARS),
              proportions = None
            ),
            amount = 10,
            activationsAmount = 100,
            exclusivityParams = ExclusivityParams(
              resolution = false,
              exclusivityPercent = 10,
              threshold = 60
            )
          )
        )

      (rewardService.isPlacementDiscountAvailable _)
        .expects(
          LoyaltyLevel.HalfYearLoyaltyLevel,
          LoyaltyRewardParams(
            Some(ExtraBonus.OVER_2000_CARS),
            hasFullStock = true,
            regionId = regionId
          )
        )
        .returningZ(true)

      (loyaltyReportService.actualizeReports _)
        .expects(
          NonEmptyList
            .of(
              LoyaltyReportActualization(
                id = 1,
                extrabonus = params.extrabonus,
                hasFullStock = params.hasFullStock,
                cashbackPercent = 10,
                cashbackAmount = 10,
                placementDiscount = Some(25),
                updatedItems = List(
                  LoyaltyReportItem(
                    data = LoyaltyReportItemData(
                      criterion = LoyaltyCriteria.ExtraBonus.toString,
                      value = 4,
                      resolution = true,
                      comment = Some(
                        "Дополнительный кешбэк за 2000+ автомобилей на складах"
                      ),
                      epoch = new DateTime(0)
                    ),
                    reportId = 1
                  ),
                  LoyaltyReportItem(
                    data = LoyaltyReportItemData(
                      criterion = LoyaltyCriteria.FullStock.toString,
                      value = 1,
                      resolution = true,
                      comment = Some("Склады заполнены"),
                      epoch = new DateTime(0)
                    ),
                    reportId = 1
                  )
                )
              )
            )
        )
        .returningZ(unit)

      processor
        .process(update)
        .provideConstantClock(new DateTime(0))
        .success
        .value
    }
  }
}
