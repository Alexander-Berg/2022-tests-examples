package ru.auto.salesman.service.impl

import cats.data.NonEmptyList
import org.joda.time.DateTime
import ru.auto.salesman.client.PromocoderClient
import ru.auto.salesman.client.cabinet.CabinetClient
import ru.auto.salesman.dao.LoyaltyReportDao
import ru.auto.salesman.dao.LoyaltyReportDao.{
  ActiveApprovedReportInfo,
  ActiveNegativeReportInfo,
  NotApprovedReportInfo
}
import ru.auto.salesman.model._
import ru.auto.salesman.model.cashback.ApiModel.{LoyaltyReport, _}
import ru.auto.salesman.model.loyalty.PromocoderBatchId
import ru.auto.salesman.service.CashbackPeriodService
import ru.auto.salesman.service.LoyaltyReportService.EmptyCashbackListException
import ru.auto.salesman.service.RewardService.CashbackProportions
import ru.auto.salesman.service.impl.LoyaltyReportServiceImpl._
import ru.auto.salesman.test.BaseSpec
import taggedtypes.TaggingExtensions

import scala.collection.mutable.ArrayBuffer

class LoyaltyReportServiceImplSpec extends BaseSpec {
  private val loyaltyReportDao = mock[LoyaltyReportDao]
  private val cabinetClient = mock[CabinetClient]
  private val cashbackPeriodService = mock[CashbackPeriodService]
  private val promocoderClient = mock[PromocoderClient]

  private val service =
    new LoyaltyReportServiceImpl(
      loyaltyReportDao,
      cabinetClient,
      cashbackPeriodService,
      promocoderClient
    )

  private val DummyFeatureCount = FeatureCount(66, FeatureUnits.Items)

  // в тесте на моках фичи на возможное удаление не обязаны совпадать с теми, что "создавались"
  private def dummyFeature(id: FeatureInstanceId, origin: String) =
    FeatureInstance(
      id,
      FeatureOrigin(origin),
      "some_test_tag",
      "test_user",
      DummyFeatureCount,
      new DateTime,
      new DateTime().plus(1),
      FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        None,
        Some(FeatureDiscount(FeatureDiscountTypes.Percent, 20)),
        None
      )
    )

  "LoyaltyReportServiceImpl" should {
    "set pre_approve and return empty result if all records were updated" in {
      val periodId = PeriodId(1)
      val clientId1 = 20101
      val clientId2 = 16543
      val statusUpdate = List(
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId1)
          .setCashbackAmount(100)
          .build(),
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId2)
          .setCashbackAmount(100)
          .build()
      )

      (loyaltyReportDao.setPreApprove _)
        .expects(*, *, *)
        .returningZ(Nil)

      service
        .setPreApprove(StaffId("test_user"), periodId, statusUpdate)
        .success
        .value shouldBe Nil
    }

    "return not pre_approved clients" in {
      val periodId = PeriodId(1)
      val userId = StaffId("test_user")
      val clientId = 20101
      val clientId2 = 16543
      val statusUpdate = List(
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId)
          .setCashbackAmount(100)
          .build(),
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId2)
          .setCashbackAmount(100)
          .build()
      )

      val clientRejections = List(
        RejectedCashbacks
          .newBuilder()
          .setClientId(clientId2)
          .setReason(
            InvalidContent
              .newBuilder()
              .setActualCashbackAmount(200)
              .setActualStatus(LoyaltyReportStatus.APPLIED)
          )
          .build()
      )

      (loyaltyReportDao.setPreApprove _)
        .expects(userId, periodId, NonEmptyList.fromListUnsafe(statusUpdate))
        .returningZ(
          List(
            NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 200)
          )
        )

      service
        .setPreApprove(userId, periodId, statusUpdate)
        .success
        .value shouldBe clientRejections
    }

    "fail on empty updates list" in {
      service
        .setPreApprove(StaffId("test_user"), PeriodId(1), Nil)
        .failure
        .exception shouldBe an[EmptyCashbackListException]
      service
        .removePreApprove(PeriodId(1), Nil)
        .failure
        .exception shouldBe an[EmptyCashbackListException]
      service
        .setApprove(StaffId("test_user"), PeriodId(1), Nil)
        .failure
        .exception shouldBe an[EmptyCashbackListException]
    }

    "remove pre_approve if possible" in {
      val periodId = PeriodId(1)
      val clientId = 20101
      val clientId2 = 16543
      val statusUpdate = List(
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId)
          .setCashbackAmount(100)
          .build(),
        ClientCashbackInfo
          .newBuilder()
          .setClientId(clientId2)
          .setCashbackAmount(100)
          .build()
      )

      val clientRejections = List(
        RejectedCashbacks
          .newBuilder()
          .setClientId(clientId2)
          .setReason(
            InvalidContent
              .newBuilder()
              .setActualCashbackAmount(200)
              .setActualStatus(LoyaltyReportStatus.APPLIED)
          )
          .build()
      )

      (loyaltyReportDao.removePreApprove _)
        .expects(periodId, NonEmptyList.fromListUnsafe(statusUpdate))
        .returningZ(
          List(
            NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 200)
          )
        )

      service
        .removePreApprove(periodId, statusUpdate)
        .success
        .value shouldBe clientRejections
    }

    "apply positive loyalty" in {
      val now = DateTime.now();
      val periodId = PeriodId(1)
      val discountBatchId = PromocoderBatchId(periodId, Some(now))
      val previousPeriodId = PeriodId(0)
      (loyaltyReportDao.findActiveApproved _)
        .expects()
        .returningZ(
          List(
            ActiveApprovedReportInfo(1, 1, periodId, 100, None, 0, None, true, now),
            ActiveApprovedReportInfo(2, 2, periodId, 120, None, 0, None, true, now),
            ActiveApprovedReportInfo(3, 3, periodId, 2000, None, 0, None, true, now),
            ActiveApprovedReportInfo(
              id = 4,
              clientId = 4,
              periodId = periodId,
              cashbackAmount = 10,
              proportions = CashbackProportions.of(100, 0).toOption.flatten,
              placementDiscountPercent = 5,
              placementDiscountOverride = None,
              resolution = true,
              now
            ),
            ActiveApprovedReportInfo(
              id = 5,
              clientId = 5,
              periodId = periodId,
              cashbackAmount = 11,
              proportions = CashbackProportions.of(50, 50).toOption.flatten,
              placementDiscountPercent = 0,
              placementDiscountOverride = None,
              resolution = true,
              now
            )
          )
        )

      (cabinetClient.setLoyalty _).expects(1L, true).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(2L, true).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(3L, true).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(4L, true).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(5L, true).returningZ(unit)

      (promocoderClient.createFeatures _)
        .expects(
          "1",
          PromocoderUser(1, UserTypes.ClientUser),
          Seq(
            FeatureInstanceRequest(
              tag = LoyaltyTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 100,
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            )
          )
        )
        .returningZ(Nil)
      (promocoderClient.createFeatures _)
        .expects(
          "1",
          PromocoderUser(2, UserTypes.ClientUser),
          Seq(
            FeatureInstanceRequest(
              tag = LoyaltyTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 120,
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            )
          )
        )
        .returningZ(Nil)
      (promocoderClient.createFeatures _)
        .expects(
          "1",
          PromocoderUser(3, UserTypes.ClientUser),
          Seq(
            FeatureInstanceRequest(
              tag = LoyaltyTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 2000,
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            )
          )
        )
        .returningZ(Nil)
      (promocoderClient.createFeatures _)
        .expects(
          "1",
          PromocoderUser(4, UserTypes.ClientUser),
          ArrayBuffer(
            FeatureInstanceRequest(
              tag = LoyaltyVasTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 10,
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            )
          )
        )
        .returningZ(Nil)

      (promocoderClient.createFeatures _)
        .expects(
          discountBatchId.toString,
          PromocoderUser(4, UserTypes.ClientUser),
          ArrayBuffer(
            FeatureInstanceRequest(
              tag = LoyaltyPlacementTag,
              startTime = None,
              lifetime = PromocodeItemsLifetime,
              count = UnlimitedFeature,
              jsonPayload = FeaturePayload(
                FeatureUnits.Items,
                FeatureTypes.Loyalty,
                discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 5))
              )
            )
          )
        )
        .returningZ(Nil)

      (promocoderClient.createFeatures _)
        .expects(
          "1",
          PromocoderUser(5, UserTypes.ClientUser),
          ArrayBuffer(
            FeatureInstanceRequest(
              tag = LoyaltyPlacementTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 6, // round up
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            ),
            FeatureInstanceRequest(
              tag = LoyaltyVasTag,
              startTime = None,
              lifetime = PromocodeMoneyLifetime,
              count = 5, // round down
              jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
            )
          )
        )
        .returningZ(Nil)

      val ActualBatchId = PromocoderBatchId(periodId, None).toString
      val NotActualBatch = PromocoderBatchId(previousPeriodId, None).toString

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(1, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("1-1", ActualBatchId),
            dummyFeature("1-2", NotActualBatch)
          )
        )

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(2, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("2-1", ActualBatchId),
            dummyFeature("2-2", NotActualBatch)
          )
        )

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(3, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("3-1", ActualBatchId),
            dummyFeature("3-2", NotActualBatch)
          )
        )

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(4, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("4-1", ActualBatchId),
            dummyFeature("4-2", NotActualBatch)
          )
        )

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(5, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("5-1", ActualBatchId),
            dummyFeature("5-2", NotActualBatch)
          )
        )

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("1-2", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("2-2", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("3-2", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("4-2", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("5-2", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(1L))
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(2L))
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(3L))
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(4L))
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(5L))
        .returningZ(unit)

      service.applyPositiveLoyalty.success.value
    }

    "apply positive loyalty for report with zero cashback" in {
      val periodId = PeriodId(1)
      val previousPeriod = PeriodId(0)
      val now = DateTime.now()
      val previousPeriodBatchId = PromocoderBatchId(previousPeriod, Some(now))
      val thisPeriodBatchId = PromocoderBatchId(periodId, Some(now))
      (loyaltyReportDao.findActiveApproved _)
        .expects()
        .returningZ(
          List(ActiveApprovedReportInfo(1, 1, periodId, 0, None, 0, None, true, now))
        )

      (cabinetClient.setLoyalty _).expects(1L, true).returningZ(unit)

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(1, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("someId", origin = previousPeriodBatchId.toString),
            dummyFeature("anotherId", origin = thisPeriodBatchId.toString)
          )
        )

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects("someId", "positive_loyalty", DummyFeatureCount)
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.of(1L))
        .returningZ(unit)

      service.applyPositiveLoyalty.success.value
    }

    "apply positive loyalty with placement discount override" in {
      val now = DateTime.now()
      val periodId = PeriodId(1)
      val discountBatchId = PromocoderBatchId(periodId, Some(now))
      (loyaltyReportDao.findActiveApproved _)
        .expects()
        .returningZ(
          List(
            ActiveApprovedReportInfo(
              id = 1,
              clientId = 1,
              periodId = periodId,
              cashbackAmount = 0,
              proportions = None,
              placementDiscountPercent = 5,
              placementDiscountOverride = Some(10),
              resolution = true,
              now
            ),
            ActiveApprovedReportInfo(
              id = 2,
              clientId = 2,
              periodId = periodId,
              cashbackAmount = 0,
              proportions = None,
              placementDiscountPercent = 0,
              placementDiscountOverride = Some(20),
              resolution = true,
              now
            )
          )
        )

      (cabinetClient.setLoyalty _).expects(1L, true).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(2L, true).returningZ(unit)

      (promocoderClient.createFeatures _)
        .expects(
          discountBatchId.toString,
          PromocoderUser(1, UserTypes.ClientUser),
          ArrayBuffer(
            FeatureInstanceRequest(
              tag = LoyaltyPlacementTag,
              startTime = None,
              lifetime = PromocodeItemsLifetime,
              count = UnlimitedFeature,
              jsonPayload = FeaturePayload(
                FeatureUnits.Items,
                FeatureTypes.Loyalty,
                discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 10))
              )
            )
          )
        )
        .returningZ(Nil)

      (promocoderClient.createFeatures _)
        .expects(
          discountBatchId.toString,
          PromocoderUser(2, UserTypes.ClientUser),
          ArrayBuffer(
            FeatureInstanceRequest(
              tag = LoyaltyPlacementTag,
              startTime = None,
              lifetime = PromocodeItemsLifetime,
              count = UnlimitedFeature,
              jsonPayload = FeaturePayload(
                FeatureUnits.Items,
                FeatureTypes.Loyalty,
                discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 20))
              )
            )
          )
        )
        .returningZ(Nil)

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(1, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("1", discountBatchId.toString)
          )
        )
      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(2, UserTypes.ClientUser))
        .returningZ(
          List(
            dummyFeature("2", discountBatchId.toString)
          )
        )

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(1L))
        .returningZ(unit)

      (loyaltyReportDao.markApplied _)
        .expects(NonEmptyList.one(2L))
        .returningZ(unit)

      service.applyPositiveLoyalty.success.value
    }

    "apply negative loyalty" in {
      def createFeatureInstance(
          id: FeatureInstanceId,
          tag: FeatureTag,
          payload: FeaturePayload = FeaturePayload(FeatureUnits.Money)
      ): FeatureInstance =
        FeatureInstance(
          id = id,
          origin = FeatureOrigin("test_origin"),
          tag = tag,
          user = "test_user",
          count = FeatureCount(1, FeatureUnits.Money),
          createTs = DateTime.now().minusDays(1),
          deadline = DateTime.now().plusDays(1),
          payload = payload
        )

      val periodId = PeriodId(1)

      (loyaltyReportDao.findActiveNegative _)
        .expects()
        .returningZ(
          List(
            ActiveNegativeReportInfo(1, 1, periodId),
            ActiveNegativeReportInfo(2, 2, periodId),
            ActiveNegativeReportInfo(3, 3, periodId)
          )
        )

      (cabinetClient.setLoyalty _).expects(1L, false).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(2L, false).returningZ(unit)
      (cabinetClient.setLoyalty _).expects(3L, false).returningZ(unit)

      val loyaltyFeatureInstance =
        createFeatureInstance("id1", FeatureInstance.LoyaltyTag)
      val loyaltyVasFeatureInstance =
        createFeatureInstance("id2", FeatureInstance.LoyaltyVasTag)
      val loyaltyDiscountFeatureInstance = createFeatureInstance(
        "id3",
        FeatureInstance.LoyaltyPlacementTag,
        payload = FeaturePayload(
          FeatureUnits.Items,
          FeatureTypes.Loyalty,
          discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, 10))
        )
      )
      val notInterestingFeature =
        createFeatureInstance("id4", FeatureInstance.LoyaltyPlacementTag)

      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(1L, UserTypes.ClientUser))
        .returningZ(List(loyaltyFeatureInstance))
      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(2L, UserTypes.ClientUser))
        .returningZ(
          List(
            loyaltyVasFeatureInstance,
            loyaltyDiscountFeatureInstance,
            notInterestingFeature
          )
        )
      (promocoderClient.getFeatures _)
        .expects(PromocoderUser(3L, UserTypes.ClientUser))
        .returningZ(Nil)

      (promocoderClient.changeFeatureCountIdempotent _)
        .expects(
          loyaltyFeatureInstance.id,
          "negative_loyalty",
          loyaltyFeatureInstance.count
        )
        .returningZ(())
      (promocoderClient.changeFeatureCountIdempotent _)
        .expects(
          loyaltyVasFeatureInstance.id,
          "negative_loyalty",
          loyaltyVasFeatureInstance.count
        )
        .returningZ(())
      (promocoderClient.changeFeatureCountIdempotent _)
        .expects(
          loyaltyDiscountFeatureInstance.id,
          "negative_loyalty",
          loyaltyDiscountFeatureInstance.count
        )
        .returningZ(())

      (loyaltyReportDao.markNegativePushed _)
        .expects(NonEmptyList.one(1L))
        .returningZ(unit)
      (loyaltyReportDao.markNegativePushed _)
        .expects(NonEmptyList.one(2L))
        .returningZ(unit)
      (loyaltyReportDao.markNegativePushed _)
        .expects(NonEmptyList.one(3L))
        .returningZ(unit)

      service.applyNegativeLoyalty.success.value
    }
  }

  "set approve and return empty result if all records were updated" in {
    val periodId = PeriodId(1)
    val clientId1 = 20101
    val clientId2 = 16543
    val statusUpdate = List(
      ClientCashbackInfo
        .newBuilder()
        .setClientId(clientId1)
        .setCashbackAmount(100)
        .build(),
      ClientCashbackInfo
        .newBuilder()
        .setClientId(clientId2)
        .setCashbackAmount(100)
        .build()
    )

    (loyaltyReportDao.setApprove _)
      .expects(*, *, *)
      .returningZ(Nil)

    service
      .setApprove(StaffId("test_user"), periodId, statusUpdate)
      .success
      .value shouldBe Nil
  }

  "return not approved clients" in {
    val periodId = PeriodId(1)
    val userId = StaffId("test_user")
    val clientId = 20101
    val clientId2 = 16543
    val statusUpdate = List(
      ClientCashbackInfo
        .newBuilder()
        .setClientId(clientId)
        .setCashbackAmount(100)
        .build(),
      ClientCashbackInfo
        .newBuilder()
        .setClientId(clientId2)
        .setCashbackAmount(100)
        .build()
    )

    val clientRejections = List(
      RejectedCashbacks
        .newBuilder()
        .setClientId(clientId2)
        .setReason(
          InvalidContent
            .newBuilder()
            .setActualCashbackAmount(200)
            .setActualStatus(LoyaltyReportStatus.APPLIED)
        )
        .build()
    )

    (loyaltyReportDao.setApprove _)
      .expects(userId, periodId, NonEmptyList.fromListUnsafe(statusUpdate))
      .returningZ(
        List(NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 200))
      )

    service
      .setApprove(userId, periodId, statusUpdate)
      .success
      .value shouldBe clientRejections
  }

  "createFeatureInstances" should {

    "create a promocode with zero placementCashback" in {
      val (cashback, discount) = service.createFeatureInstances(
        vasCashback = 5,
        placementCashback = 0,
        placementDiscountPercent = 10
      )
      cashback.size shouldBe 1
      cashback should contain(createMoneyFeatureInstanceRequest(LoyaltyVasTag, 5))
      discount.size shouldBe 1
      discount should contain(createItemsFeatureInstanceRequest(LoyaltyPlacementTag, 10))
    }

    "create a promocode with positive placementCashback" in {
      val (cashback, discount) = service.createFeatureInstances(
        vasCashback = 5,
        placementCashback = 50,
        placementDiscountPercent = 10
      )
      cashback.size shouldBe 1
      cashback should contain(createMoneyFeatureInstanceRequest(LoyaltyVasTag, 5))
      discount.size shouldBe 1
      discount should contain(createItemsFeatureInstanceRequest(LoyaltyPlacementTag, 10))
    }

    "create a promocode with zero placementDiscountPercent" in {
      val (cashback, discount) = service.createFeatureInstances(
        vasCashback = 5,
        placementCashback = 50,
        placementDiscountPercent = 0
      )

      cashback.size shouldBe 2
      cashback should contain allOf (createMoneyFeatureInstanceRequest(
        LoyaltyPlacementTag,
        50
      ),
      createMoneyFeatureInstanceRequest(LoyaltyVasTag, 5))
      discount shouldBe empty
    }

    "create a promocode with zero placementDiscountPercent and placementCashback" in {
      val (cashback, discount) = service.createFeatureInstances(
        vasCashback = 5,
        placementCashback = 0,
        placementDiscountPercent = 0
      )
      cashback.size shouldBe 1
      cashback should contain(createMoneyFeatureInstanceRequest(LoyaltyVasTag, 5))
      discount shouldBe empty
    }

    "create a promocode with all zeros" in {
      val (cashback, discount) = service.createFeatureInstances(
        vasCashback = 0,
        placementCashback = 0,
        placementDiscountPercent = 0
      )
      cashback shouldBe empty
      discount shouldBe empty
    }
  }

  "revoke promocodes and set resolution to previous period value" in {
    val clientId = 1L
    val currentPeriodId = 2L @@ PeriodId
    val previousPeriodId = 3L
    val resolution = true
    val userId = StaffId("test_user")
    val now = DateTime.now()
    val promocodeUser = PromocoderUser(clientId, UserTypes.ClientUser)
    val featureInstance = FeatureInstance(
      id = "1",
      origin = FeatureOrigin("origin"),
      tag = FeatureInstance.LoyaltyVasTag,
      user = "user",
      count = FeatureCount(1L, FeatureUnits.Money),
      createTs = now,
      deadline = now.plusDays(1),
      payload = FeaturePayload(FeatureUnits.Money)
    )

    (loyaltyReportDao.findCurrent _)
      .expects(clientId)
      .returningZ(
        Some(
          LoyaltyReport
            .newBuilder()
            .setPeriodId(currentPeriodId)
            .setStatus(LoyaltyReportStatus.APPROVED)
            .build()
        )
      )

    (loyaltyReportDao.findPrevious _)
      .expects(clientId)
      .returningZ(
        Some(
          LoyaltyReport
            .newBuilder()
            .setPeriodId(previousPeriodId)
            .setResolution(resolution)
            .build()
        )
      )

    (cabinetClient.setLoyalty _)
      .expects(clientId, resolution)
      .returningZ(())

    (promocoderClient.getFeatures _)
      .expects(promocodeUser)
      .returningZ(List(featureInstance, featureInstance.copy(tag = "invalid")))

    (promocoderClient.changeFeatureCount _)
      .expects(featureInstance.id, featureInstance.count)
      .returningZ(featureInstance)

    (loyaltyReportDao.revoke _)
      .expects(clientId, currentPeriodId, userId)
      .returningZ(())

    service.revoke(clientId, currentPeriodId, userId).success.value
  }

  private def createMoneyFeatureInstanceRequest(tag: FeatureTag, count: Long) =
    FeatureInstanceRequest(
      tag = tag,
      startTime = None,
      lifetime = PromocodeMoneyLifetime,
      count = count,
      jsonPayload = FeaturePayload(FeatureUnits.Money, FeatureTypes.Loyalty)
    )

  private def createItemsFeatureInstanceRequest(tag: FeatureTag, percent: Int) =
    FeatureInstanceRequest(
      tag = tag,
      startTime = None,
      lifetime = PromocodeItemsLifetime,
      count = UnlimitedFeature,
      jsonPayload = FeaturePayload(
        FeatureUnits.Items,
        FeatureTypes.Loyalty,
        discount = Some(FeatureDiscount(FeatureDiscountTypes.Percent, percent))
      )
    )

}
