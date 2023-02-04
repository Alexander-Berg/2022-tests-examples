package ru.auto.salesman.service.impl

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfter
import ru.auto.cabinet.ApiModel
import ru.auto.cabinet.api_model.ExtraBonus
import ru.auto.cabinet.palma.proto.cashback_policies_palma_model.{
  CashbackPolicies,
  CashbackPolicy,
  CashbackUsageRule,
  ExtraBonuses
}
import ru.auto.salesman.model.{ClientId, PeriodId, RegionId}
import ru.auto.salesman.model.cashback.{CashbackPeriod, ExclusivityParams, LoyaltyLevel}
import ru.auto.salesman.model.cashback.LoyaltyLevel._
import ru.auto.salesman.service.{DealerStatsService, LoyaltyParamsFetcher}
import ru.auto.salesman.service.RewardService.{
  Cashback,
  CashbackParams,
  CashbackPercent,
  CashbackProportions,
  IncorrectCashbackPercentSum
}
import ru.auto.salesman.service.LoyaltyParamsFetcher.{
  LoyaltyParams,
  PlacementDiscountResult
}
import ru.auto.salesman.service.palma.PalmaService
import ru.auto.salesman.service.palma.domain.CallCashbackPoliciesIndex
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.util.GeoUtils._
import ru.yandex.vertis.moderation.proto.Model.Metadata.DealerMetadata.LoyaltyInfo
import zio.{Task, ZIO}

class RewardServiceSpec extends BaseSpec with BeforeAndAfter {

  private val defaultValue = CashbackPolicy(
    cashbackPercent = 10,
    cashbackPercentOnFullStock = 20,
    availableFor = List(CashbackUsageRule.VAS_CARS_USED),
    extraBonuses = Seq.empty[ExtraBonuses],
    vasSpentPercent = 50,
    placementSpendPercent = 50
  )

  private val palmaService = new PalmaService {

    val aggregatePolicy = CashbackPolicies(
      exclusivityPercent = 5,
      defaultCashbackPolicy = Some(defaultValue),
      regionCashbackPolicies = List(
        CashbackPolicy(
          regionId = RegMoscow.toInt,
          cashbackPercent = 0,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          ),
          vasSpentPercent = 50,
          placementSpendPercent = 50
        ),
        CashbackPolicy(
          regionId = RegSPb.toInt,
          cashbackPercent = 0,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          ),
          vasSpentPercent = 50,
          placementSpendPercent = 50
        ),
        CashbackPolicy(
          regionId = RegAltay.toInt,
          cashbackPercent = 10,
          cashbackPercentOnFullStock = 20,
          availableFor = List(CashbackUsageRule.ALL_CARS_USED),
          extraBonuses = Seq.empty[ExtraBonuses],
          vasSpentPercent = 50,
          placementSpendPercent = 50
        )
      )
    )

    def getCashbackPolicies: Task[CashbackPolicies] =
      ZIO.succeed(aggregatePolicy)

    def getCallCashbackPolicies: Task[CallCashbackPoliciesIndex] = ???
  }

  private val palmaServiceWithIncorrectCashbackPercentSum = new PalmaService {

    val aggregatePolicy = CashbackPolicies(
      exclusivityPercent = 5,
      defaultCashbackPolicy = Some(defaultValue.copy(vasSpentPercent = 123)),
      regionCashbackPolicies = List(
        CashbackPolicy(
          regionId = RegMoscow.toInt,
          cashbackPercent = 0,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          ),
          vasSpentPercent = 1,
          placementSpendPercent = 2
        ),
        CashbackPolicy(
          regionId = RegSPb.toInt,
          cashbackPercent = 0,
          cashbackPercentOnFullStock = 5,
          availableFor = List(CashbackUsageRule.VAS_CARS_USED),
          extraBonuses = Seq(
            ExtraBonuses(ExtraBonus.UNDER_500_CARS, 3),
            ExtraBonuses(ExtraBonus.UNDER_1000_CARS, 5),
            ExtraBonuses(ExtraBonus.UNDER_2000_CARS, 7),
            ExtraBonuses(ExtraBonus.OVER_2000_CARS, 10)
          ),
          vasSpentPercent = 3,
          placementSpendPercent = 4
        ),
        CashbackPolicy(
          regionId = RegAltay.toInt,
          cashbackPercent = 10,
          cashbackPercentOnFullStock = 20,
          availableFor = List(CashbackUsageRule.ALL_CARS_USED),
          extraBonuses = Seq.empty[ExtraBonuses],
          vasSpentPercent = 5,
          placementSpendPercent = 6
        )
      )
    )

    def getCashbackPolicies: Task[CashbackPolicies] =
      ZIO.succeed(aggregatePolicy)

    def getCallCashbackPolicies: Task[CallCashbackPoliciesIndex] = ???
  }

  private val loyaltyParamsFetcher = mock[LoyaltyParamsFetcher]
  private val dealerStatsService = mock[DealerStatsService]
  private val mockedPalmaService = mock[PalmaService]

  private val cashbackService =
    new RewardServiceImpl(
      palmaService,
      loyaltyParamsFetcher,
      dealerStatsService
    )

  private val cashbackServiceWithIncorrectPalmaData =
    new RewardServiceImpl(
      palmaServiceWithIncorrectCashbackPercentSum,
      loyaltyParamsFetcher,
      dealerStatsService
    )

  private val rewardServiceWithMockedPalma =
    new RewardServiceImpl(
      mockedPalmaService,
      loyaltyParamsFetcher,
      dealerStatsService
    )

  private val period = CashbackPeriod(
    id = PeriodId(10),
    start = DateTime.now().minusMonths(1),
    finish = DateTime.now(),
    isActive = false,
    previousPeriod = None
  )

  "RewardService" should {

    "return zero cashback if less than 6 months since first moderation date" in {
      mockActivations()
      mockParameters(
        Map(
          1L -> LoyaltyParams(
            extrabonus = None,
            hasFullStock = false,
            period = period,
            regionId = RegAltay,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          )
        )
      )
      cashbackService
        .resolveCashback(1L, SmallestLoyalty.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "return cashback only for regions if more than 6 and less than 12 months since first moderation date" in {
      mockActivations()
      val params = Map(
        1L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = false,
          period = period,
          regionId = RegAltay,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        2L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = false,
          period = period,
          regionId = RegSPb,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        20101L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegMoscow,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        )
      )
      mockParameters(params)

      cashbackService
        .resolveCashback(1, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 10,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 30,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(20101, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(2, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "return default cashback for regions if cashback policy not found" in {
      mockActivations()
      mockParameters(
        Map(
          1L -> LoyaltyParams(
            extrabonus = None,
            hasFullStock = false,
            period = period,
            regionId = RegAltay,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          )
        )
      )

      cashbackService
        .resolveCashback(1L, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = defaultValue.cashbackPercent,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 30,
        activationsAmount = 30 / defaultValue.cashbackPercent * 100,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "throw error IncorrectCashbackPercentSum" in {
      mockActivations()
      mockParameters(
        Map(
          1L -> LoyaltyParams(
            extrabonus = None,
            hasFullStock = false,
            period = period,
            regionId = RegAltay,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          )
        )
      )

      cashbackServiceWithIncorrectPalmaData
        .resolveCashback(1L, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .failure
        .exception shouldBe IncorrectCashbackPercentSum(5, 6)
    }

    "return bigger cashback if full stock and more than 12 months since first moderation date" in {
      mockActivations()
      val params = Map(
        1L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegAltay,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        20101L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegMoscow,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        )
      )
      mockParameters(params)

      cashbackService
        .resolveCashback(1, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 60,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(20101, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 5,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 15,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "return cashback with extrabonus" in {
      mockActivations()
      mockParameters(
        Map(
          1L -> LoyaltyParams(
            extrabonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
            hasFullStock = true,
            period = period,
            regionId = RegAltay,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          ),
          2L -> LoyaltyParams(
            extrabonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
            hasFullStock = false,
            period = period,
            regionId = RegSPb,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          ),
          20101L -> LoyaltyParams(
            extrabonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
            hasFullStock = true,
            period = period,
            regionId = RegMoscow,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          )
        )
      )

      cashbackService
        .resolveCashback(1, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 60,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(20101L, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(2, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      cashbackService
        .resolveCashback(20101, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        CashbackPercent(
          percent = 10,
          hasFullStock = true,
          extraBonus = Some(ApiModel.ExtraBonus.UNDER_1000_CARS),
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        30,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "return zero cashback amount for client without activations" in {
      val params = Map(
        1L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegAltay,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        )
      )
      mockParameters(params)

      mockActivations(0)

      cashbackService
        .resolveCashback(1, YearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 0.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "resolve cashback by client id and raw loyalty" in {
      mockParameters(
        Map(
          1L -> LoyaltyParams(
            extrabonus = None,
            hasFullStock = true,
            period = period,
            regionId = RegAltay,
            managerFio = None,
            placementDiscountResult = PlacementDiscountResult.Discount(0)
          )
        )
      )
      mockActivations(0)

      cashbackService
        .resolveCashback(1, 12, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 0.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "not resolve cashback for invalid loyalty" in {
      cashbackService
        .resolveCashback(20101, 13, PeriodId(10))
        .failure
        .cause
        .squash shouldBe a[IllegalLoyaltyLevel]
    }

    "not call loyalty params, if presented" in {

      mockActivations()

      val params = LoyaltyParams(
        extrabonus = Some(ApiModel.ExtraBonus.OVER_2000_CARS),
        hasFullStock = false,
        period = CashbackPeriod(
          id = PeriodId(10),
          start = new DateTime(),
          finish = new DateTime(),
          isActive = true,
          previousPeriod = None
        ),
        regionId = RegionId(1),
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(0)
      )

      cashbackService
        .resolveCashback(
          1,
          LoyaltyLevel.YearLoyaltyLevel,
          PeriodId(10),
          params,
          None
        )
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = Some(ApiModel.ExtraBonus.OVER_2000_CARS),
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }

    "include exclusive bonus, if presented for non-zero loyalty" in {

      mockActivations()
      val loyaltyInfo = LoyaltyInfo
        .newBuilder()
        .setExclusiveOffers(10)
        .setPeriodOffers(10)
        .build()

      val params = LoyaltyParams(
        extrabonus = Some(ApiModel.ExtraBonus.OVER_2000_CARS),
        hasFullStock = false,
        period = CashbackPeriod(
          id = PeriodId(10),
          start = new DateTime(),
          finish = new DateTime(),
          isActive = true,
          previousPeriod = None
        ),
        regionId = RegionId(1),
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(0)
      )

      LoyaltyLevel.values.filterNot(_ == LoyaltyLevel.NoLoyalty).foreach { ll =>
        val cashback = cashbackService
          .resolveCashback(1, ll, PeriodId(10), params, Some(loyaltyInfo))
          .success
          .value

        cashback.percent.percent shouldBe 5
        cashback.exclusivityParams shouldBe ExclusivityParams(
          resolution = true,
          exclusivityPercent = 100,
          threshold = 60
        )
      }
    }

    "don't include exclusive bonus for zero loyalty" in {

      mockActivations()
      val loyaltyInfo = LoyaltyInfo
        .newBuilder()
        .setExclusiveOffers(10)
        .setPeriodOffers(10)
        .build()

      val params = LoyaltyParams(
        extrabonus = None,
        hasFullStock = false,
        period = CashbackPeriod(
          id = PeriodId(10),
          start = new DateTime(),
          finish = new DateTime(),
          isActive = true,
          previousPeriod = None
        ),
        regionId = RegionId(1),
        managerFio = None,
        placementDiscountResult = PlacementDiscountResult.Discount(0)
      )

      cashbackService
        .resolveCashback(
          1,
          LoyaltyLevel.NoLoyalty,
          PeriodId(10),
          params,
          Some(loyaltyInfo)
        )
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(
          resolution = true,
          exclusivityPercent = 100,
          threshold = 60
        )
      )
    }
  }

  "RewardService new cashback policy" should {
    def genCashbackPolicy(
        regionId: RegionId,
        useMinLoyaltyLevelForCashback: Boolean,
        minLoyaltyLevelForCashback: LoyaltyLevel
    ): CashbackPolicy =
      defaultValue.copy(
        regionId = regionId.toInt,
        useMinLoyaltyLevelForCashback = useMinLoyaltyLevelForCashback,
        minLoyaltyLevelForCashback = minLoyaltyLevelForCashback.raw
      )

    def cashbackPolicies(policies: Seq[CashbackPolicy]): CashbackPolicies =
      CashbackPolicies(regionCashbackPolicies = policies)

    "return cashback if loyalty level GE than min level" in {
      mockActivations()
      val params = Map(
        1L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = false,
          period = period,
          regionId = RegAltay,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        2L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = false,
          period = period,
          regionId = RegSPb,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        3L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegKaluga,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        4L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegChuvashiya,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        ),
        20101L -> LoyaltyParams(
          extrabonus = None,
          hasFullStock = true,
          period = period,
          regionId = RegMoscow,
          managerFio = None,
          placementDiscountResult = PlacementDiscountResult.Discount(0)
        )
      )
      mockParameters(params)

      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .atLeastOnce()
        .returningZ {
          cashbackPolicies {
            Seq(
              genCashbackPolicy(
                RegMoscow,
                useMinLoyaltyLevelForCashback = true,
                minLoyaltyLevelForCashback = HalfYearLoyaltyLevel
              ),
              genCashbackPolicy(
                RegSPb,
                useMinLoyaltyLevelForCashback = true,
                minLoyaltyLevelForCashback = YearLoyaltyLevel
              ),
              genCashbackPolicy(
                RegAltay,
                useMinLoyaltyLevelForCashback = true,
                minLoyaltyLevelForCashback = SmallestLoyalty
              ),
              genCashbackPolicy(
                RegKaluga,
                useMinLoyaltyLevelForCashback = true,
                minLoyaltyLevelForCashback = NoLoyalty
              ),
              genCashbackPolicy(
                RegChuvashiya,
                useMinLoyaltyLevelForCashback = true,
                minLoyaltyLevelForCashback = SmallestLoyalty
              )
            )
          }
        }

      rewardServiceWithMockedPalma
        .resolveCashback(2, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      rewardServiceWithMockedPalma
        .resolveCashback(4, NoLoyalty.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 0,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 0,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      rewardServiceWithMockedPalma
        .resolveCashback(20101, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 60,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      rewardServiceWithMockedPalma
        .resolveCashback(3, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 20,
          hasFullStock = true,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 60,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )

      rewardServiceWithMockedPalma
        .resolveCashback(1, HalfYearLoyaltyLevel.raw, PeriodId(10))
        .success
        .value shouldBe Cashback(
        percent = CashbackPercent(
          percent = 10,
          hasFullStock = false,
          extraBonus = None,
          proportions = CashbackProportions.of(50, 50).toOption.flatten
        ),
        amount = 30,
        activationsAmount = 300.0,
        exclusivityParams = ExclusivityParams(resolution = false, 0, 60)
      )
    }
  }

  "RewardService.isPlacementDiscountAvailable" should {
    def genCashbackPoliciesWith(
        regionId: RegionId,
        minLoyaltyLevel: LoyaltyLevel
    ): CashbackPolicies =
      CashbackPolicies(
        regionCashbackPolicies = Seq(
          defaultValue.copy(
            regionId = regionId.toInt,
            minLoyaltyLevelForDiscount = minLoyaltyLevel.raw
          )
        )
      )

    "return TRUE for capitals and EQ loyalty level with full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegMoscow,
            minLoyaltyLevel = LoyaltyLevel.SmallestLoyalty
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegMoscow
          )
        )
        .success
        .value shouldBe true
    }

    "return TRUE for capitals and GT loyalty level with full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegMoscow,
            minLoyaltyLevel = LoyaltyLevel.SmallestLoyalty
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.HalfYearLoyaltyLevel,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegMoscow
          )
        )
        .success
        .value shouldBe true
    }

    "return FALSE for capitals and LT loyalty level and full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegMoscow,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegMoscow
          )
        )
        .success
        .value shouldBe false
    }

    "return FALSE for capitals and GE loyalty level and not full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegMoscow,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.YearLoyaltyLevel,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = false,
            regionId = RegMoscow
          )
        )
        .success
        .value shouldBe false
    }

    "return FALSE for capitals and LT loyalty level and not full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegMoscow,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = false,
            regionId = RegMoscow
          )
        )
        .success
        .value shouldBe false
    }

    "return TRUE for regions and EQ loyalty level with full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegKaluga,
            minLoyaltyLevel = LoyaltyLevel.SmallestLoyalty
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegKaluga
          )
        )
        .success
        .value shouldBe true
    }

    "return TRUE for regions and GT loyalty level with full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegKaluga,
            minLoyaltyLevel = LoyaltyLevel.SmallestLoyalty
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.HalfYearLoyaltyLevel,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegKaluga
          )
        )
        .success
        .value shouldBe true
    }

    "return FALSE for regions and LT loyalty level and full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegKaluga,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = true,
            regionId = RegKaluga
          )
        )
        .success
        .value shouldBe false
    }

    "return FALSE for regions and GE loyalty level and not full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegKaluga,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.YearLoyaltyLevel,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = false,
            regionId = RegKaluga
          )
        )
        .success
        .value shouldBe false
    }

    "return FALSE for regions and LT loyalty level and not full stock" in {
      (mockedPalmaService.getCashbackPolicies _)
        .expects()
        .returningZ {
          genCashbackPoliciesWith(
            regionId = RegKaluga,
            minLoyaltyLevel = LoyaltyLevel.YearLoyaltyLevel
          )
        }

      rewardServiceWithMockedPalma
        .isPlacementDiscountAvailable(
          loyaltyLevel = LoyaltyLevel.SmallestLoyalty,
          loyaltyParams = CashbackParams(
            extrabonus = None,
            hasFullStock = false,
            regionId = RegKaluga
          )
        )
        .success
        .value shouldBe false
    }
  }

  private def mockActivations(result: Int = 300): Unit =
    (dealerStatsService.getTotalSpentOnActivations _)
      .expects(*, *, *, *, *)
      .anyNumberOfTimes()
      .returningZ(result)

  private def mockParameters(params: Map[ClientId, LoyaltyParams]): Unit =
    params.foreach { case (clientId, params) =>
      (loyaltyParamsFetcher.getParams _)
        .expects(period.id, clientId)
        .returningZ(params)
        .atLeastOnce()
    }
}
