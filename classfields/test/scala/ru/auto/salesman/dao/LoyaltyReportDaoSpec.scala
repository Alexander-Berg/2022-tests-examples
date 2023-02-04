package ru.auto.salesman.dao

import cats.data.NonEmptyList
import cats.syntax.option._
import com.google.protobuf.util.Timestamps
import org.joda.time.DateTime
import ru.auto.cabinet.ApiModel.ExtraBonus
import ru.auto.salesman.dao.ClientsChangedBufferDao.DataSourceFilter
import ru.auto.salesman.dao.LoyaltyReportDao.{
  ActiveApprovedReportInfo,
  ActiveNegativeReportInfo,
  NotApprovedReportInfo
}
import ru.auto.salesman.dao.impl.jdbc.JdbcClientsChangedBufferDao.commonFilter
import ru.auto.salesman.dao.impl.jdbc.JdbcLoyaltyReportDao
import ru.auto.salesman.dao.testkit.DatabaseFunctionsDao
import ru.auto.salesman.environment._
import ru.auto.salesman.model.cashback.ApiModel.{
  ClientCashbackInfo,
  LoyaltyReport,
  LoyaltyReportStatus
}
import ru.auto.salesman.model.cashback.LoyaltyLevel.{
  HalfYearLoyaltyLevel,
  YearLoyaltyLevel
}
import ru.auto.salesman.model.cashback._
import ru.auto.salesman.model.{ClientId, Funds, Percent, PeriodId, StaffId}
import ru.auto.salesman.service.LoyaltyReportService.LoyaltyReportActualization
import ru.auto.salesman.service.RewardService.CashbackProportions
import ru.auto.salesman.test.BaseSpec
import zio.ZIO

trait LoyaltyReportDaoSpec extends BaseSpec {
  def loyaltyReportDao: TestLoyaltyReportDaoExtensions
  def cashbackPeriodDao: CashbackPeriodDao
  def clientsChangedBufferDao: ClientsChangedBufferDao
  def databaseFunctionsDao: DatabaseFunctionsDao

  "LoyaltyReportDao" should {
    "not set pre_approve on report with status=applied" in {
      val periodId1 = PeriodId(1)
      val clientId1 = 1
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      val clientId2 = 2
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      loyaltyReportDao.find(periodId1, clientId1).status shouldBe "applied"
      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "applied"

      loyaltyReportDao
        .setPreApprove(
          StaffId("test_user"),
          periodId1,
          NonEmptyList
            .of(
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
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId1, LoyaltyReportStatus.APPLIED, 100),
        NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 100)
      )

      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "applied"
      loyaltyReportDao.find(periodId1, clientId1).status shouldBe "applied"
    }

    "not set pre_approve on report pre_approved by other user" in {
      val periodId = PeriodId(1)
      val clientId = 42
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      loyaltyReportDao
        .setPreApprove(
          StaffId("test_user"),
          periodId,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe Nil

      loyaltyReportDao
        .setPreApprove(
          StaffId("test_user2"),
          periodId,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId, LoyaltyReportStatus.PRE_APPROVED, 100)
      )

      val updated = loyaltyReportDao.find(periodId, clientId)
      updated.periodId shouldBe periodId
      updated.clientId shouldBe clientId
      updated.status shouldBe "pre_approved"
      updated.preApprovedBy should contain("test_user")
      updated.preApprovedDate should not be empty
    }

    "set pre_approve on report with status = in_progress" in {
      val periodId1 = PeriodId(2)
      val clientId1 = 3
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      val clientId2 = 4
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      loyaltyReportDao
        .setPreApprove(
          StaffId("test_user"),
          periodId1,
          NonEmptyList
            .of(
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
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPROVED, 100)
      )

      val updated = loyaltyReportDao.find(periodId1, clientId1)
      updated.periodId shouldBe periodId1
      updated.clientId shouldBe clientId1
      updated.status shouldBe "pre_approved"
      updated.preApprovedBy should contain("test_user")
      updated.preApprovedDate should not be empty

      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "approved"
    }

    "remove pre approve" in {
      val periodId1 = PeriodId(3)
      val clientId1 = 4
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )

      val clientId2 = 5
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      val clientId3 = 6
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 1001,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )

      loyaltyReportDao
        .removePreApprove(
          periodId1,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId1)
                .setCashbackAmount(100)
                .build(),
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId2)
                .setCashbackAmount(100)
                .build(),
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId3)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPROVED, 100),
        NotApprovedReportInfo(clientId3, LoyaltyReportStatus.PRE_APPROVED, 1001)
      )

      val updated = loyaltyReportDao.find(periodId1, clientId1)
      updated.periodId shouldBe periodId1
      updated.clientId shouldBe clientId1
      updated.status shouldBe "in_progress"
      updated.preApprovedBy should be(empty)
      updated.preApprovedDate should be(empty)

      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "approved"
      loyaltyReportDao.find(periodId1, clientId3).status shouldBe "pre_approved"
    }

    "update existing record" in withItems { items =>
      val (clientId, periodId) = (2L, PeriodId(2))

      val now = new DateTime()

      //report from kafka
      val initialReportMsg = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(false)
        .setCashbackAmount(0L)
        .setCashbackPercent(0)
        .setOverrideUntil(Timestamps.fromMillis(now.getMillis))
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.NoLoyalty.raw)
        .setActivationsAmount(0L)
        .setHasFullStock(false)
        .setExtraBonus(ExtraBonus.OVER_2000_CARS)
        .build()

      val updateReportMsg = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(true)
        .setCashbackAmount(10L)
        .setCashbackPercent(10)
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.SmallestLoyalty.raw)
        .setActivationsAmount(100L)
        .setHasFullStock(true)
        .setAutoruExclusivePercent(10)
        .setManagerName("updated")
        .setVasSpendPercent(1)
        .setPlacementSpendPercent(2)
        .setPlacementDiscountPercent(20)
        .build()

      //upsert
      val (
        insert,
        update,
        dbItems
      ) = (for {
        _ <- loyaltyReportDao.upsert(initialReportMsg, items)
        insertResult <- loyaltyReportDao.findByPeriodID(periodId)

        //insertion with same resolution should not affect clients_changed_buffer
        _ <- loyaltyReportDao.upsert(initialReportMsg, items)
        _ <- loyaltyReportDao.upsert(updateReportMsg, items)
        _ <- loyaltyReportDao.upsert(updateReportMsg, items)

        updateResult <- loyaltyReportDao.findByPeriodID(periodId)
        insertedItems = loyaltyReportDao.findItems(insertResult.head.getId)
      } yield
        (
          insertResult,
          updateResult,
          insertedItems
        )).success.value

      {
        insert.size shouldBe 1
        val initialReport: LoyaltyReport = insert.head
        import initialReport._
        getActivationsAmount shouldBe 0
        getResolution shouldBe false
        getLoyaltyLevel shouldBe LoyaltyLevel.NoLoyalty.raw
        getCashbackAmount shouldBe 0
        getCashbackPercent shouldBe 0
        getExtraBonus shouldBe ExtraBonus.OVER_2000_CARS
        getHasFullStock shouldBe false
        getStatus shouldBe ApiModel.LoyaltyReportStatus.IN_PROGRESS_NEGATIVE
        hasOverrideUntil shouldBe true
        getManagerName shouldBe ""
        getVasSpendPercent shouldBe 0
        getPlacementSpendPercent shouldBe 0
        getPlacementDiscountPercent shouldBe 0

        Timestamps.toMillis(getOverrideUntil) shouldBe now.getMillis
      }

      {
        update.size shouldBe 1
        val updatedReport = update.head
        import updatedReport._
        getActivationsAmount shouldBe 100
        getResolution shouldBe true
        getLoyaltyLevel shouldBe LoyaltyLevel.SmallestLoyalty.raw
        getCashbackAmount shouldBe 10
        getCashbackPercent shouldBe 10
        getExtraBonus shouldBe ExtraBonus.UNKNOWN_BONUS
        getHasFullStock shouldBe true
        getStatus shouldBe ApiModel.LoyaltyReportStatus.IN_PROGRESS
        hasOverrideUntil shouldBe false
        getAutoruExclusivePercent shouldBe 10
        getManagerName shouldBe "updated"
        getVasSpendPercent shouldBe 1
        getPlacementSpendPercent shouldBe 2
        getPlacementDiscountPercent shouldBe 20

      }

      {
        val newReportId = insert.head.getId
        dbItems should contain theSameElementsAs (items.map(d =>
          LoyaltyReportItem(reportId = newReportId, data = d)
        )
        )
      }
    }

    "not update approved report" in {

      val (clientId, periodId) = (1L, PeriodId(1))
      val items = Nil
      val now = new DateTime()

      val initialReport = InitialLoyaltyReport(
        periodId = periodId,
        clientId = clientId,
        loyaltyLevel = YearLoyaltyLevel,
        cashbackAmount = 10,
        cashbackPercent = 10,
        hasFullStock = true,
        extraBonus = None
      )

      val updateReportMsg = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(false)
        .setOverrideUntil(Timestamps.fromMillis(now.getMillis))
        .setCashbackAmount(20L)
        .setCashbackPercent(20)
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.SmallestLoyalty.raw)
        .setActivationsAmount(100)
        .setExtraBonus(ExtraBonus.UNDER_2000_CARS)
        .setHasFullStock(true)
        .setManagerName("updated")
        .build()
      (for {
        _ <- ZIO {
          loyaltyReportDao
            .plainInsert(initialReport, LoyaltyReportStatus.APPLIED)
        }
        insertResult <- loyaltyReportDao.findByPeriodID(PeriodId(1))
        _ <- loyaltyReportDao.upsert(updateReportMsg, items)
        updateResult <- loyaltyReportDao.findByPeriodID(periodId)

      } yield insertResult should contain theSameElementsAs updateResult).success.value
    }

    "not set approve on report with status=applied" in {
      val periodId1 = PeriodId(1)
      val clientId1 = 7
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      val clientId2 = 8
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      loyaltyReportDao.find(periodId1, clientId1).status shouldBe "applied"
      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "applied"

      loyaltyReportDao
        .setApprove(
          StaffId("test_user"),
          periodId1,
          NonEmptyList
            .of(
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
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId1, LoyaltyReportStatus.APPLIED, 100),
        NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 100)
      )

      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "applied"
      loyaltyReportDao.find(periodId1, clientId1).status shouldBe "applied"
    }

    "not set approve on report approved by other user" in {
      val periodId = PeriodId(1)
      val clientId = 9
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      loyaltyReportDao
        .setApprove(
          StaffId("test_user"),
          periodId,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe Nil

      loyaltyReportDao
        .setApprove(
          StaffId("test_user2"),
          periodId,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId, LoyaltyReportStatus.APPROVED, 100)
      )

      val updated = loyaltyReportDao.find(periodId, clientId)
      updated.periodId shouldBe periodId
      updated.clientId shouldBe clientId
      updated.status shouldBe "approved"
      updated.approvedBy should contain("test_user")
      updated.approvedDate should not be empty
    }

    "set approve on report with status = in_progress" in {
      val periodId1 = PeriodId(2)
      val clientId1 = 10
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      val clientId2 = 11
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      val insertClientsChangedBuffer =
        clientsChangedBufferDao.get(commonFilter).success.value

      loyaltyReportDao
        .setApprove(
          StaffId("test_user"),
          periodId1,
          NonEmptyList
            .of(
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
        )
        .success
        .value shouldBe List(
        NotApprovedReportInfo(clientId2, LoyaltyReportStatus.APPLIED, 100)
      )

      val updated = loyaltyReportDao.find(periodId1, clientId1)
      updated.periodId shouldBe periodId1
      updated.clientId shouldBe clientId1
      updated.status shouldBe "approved"
      updated.approvedBy should contain("test_user")
      updated.approvedDate should not be empty

      loyaltyReportDao.find(periodId1, clientId2).status shouldBe "applied"

      val updateClientsChangedBuffer =
        clientsChangedBufferDao.get(commonFilter).success.value

      val updateClientsChangedBufferForDealerPony =
        clientsChangedBufferDao
          .get(DataSourceFilter(Set(JdbcLoyaltyReportDao.dataSourceDealerPony)))
          .success
          .value

      {
        updateClientsChangedBuffer.exists { r =>
          r.clientId == clientId1 && r.dataSource == "loyalty_report"
        } shouldBe true

        updateClientsChangedBufferForDealerPony.exists { r =>
          r.clientId == clientId1 && r.dataSource == "dealer_pony"
        } shouldBe true

        updateClientsChangedBuffer.size should be(
          insertClientsChangedBuffer.size + 1
        )

        updateClientsChangedBufferForDealerPony.size should be(
          insertClientsChangedBuffer.size + 1
        )
      }
    }

    "set approve on report with status = pre_approved" in {
      val periodId1 = PeriodId(2)
      val clientId1 = 12
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId1,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )

      loyaltyReportDao
        .setApprove(
          StaffId("test_user"),
          periodId1,
          NonEmptyList
            .of(
              ClientCashbackInfo
                .newBuilder()
                .setClientId(clientId1)
                .setCashbackAmount(100)
                .build()
            )
        )
        .success
        .value shouldBe Nil

      val updated = loyaltyReportDao.find(periodId1, clientId1)
      updated.periodId shouldBe periodId1
      updated.clientId shouldBe clientId1
      updated.status shouldBe "approved"
      updated.approvedBy should contain("test_user")
      updated.approvedDate should not be empty
    }

    "find not approved reports with active period" in {

      val (activePeriodId, closedPeriodId) = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(2),
          DateTime.now().minusDays(1)
        )
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(4),
          DateTime.now().minusDays(3)
        )
        periods <- cashbackPeriodDao.getPeriods
        active = periods.head.id
        closed = periods(1).id
        _ <- cashbackPeriodDao.closeById(closed)
      } yield (active, closed)).success.value

      val clientId1 = 15
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            closedPeriodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      loyaltyReportDao
        .findActiveNotApproved(clientId1)
        .success
        .value
        .map(_.id) should contain theSameElementsAs (List(id1))

      val clientId2 = 16
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS_NEGATIVE
        )

      loyaltyReportDao
        .findActiveNotApproved(clientId2)
        .success
        .value
        .map(_.id) should contain theSameElementsAs (List(id2))

      val clientId3 = 17
      val id3 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )

      loyaltyReportDao
        .findActiveNotApproved(clientId3)
        .success
        .value
        .map(_.id) should contain theSameElementsAs (List(id3))

      val clientId4 = 18
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId4,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      loyaltyReportDao
        .findActiveNotApproved(clientId4)
        .success
        .value shouldBe empty

      val clientId5 = 19
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId5,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      loyaltyReportDao
        .findActiveNotApproved(clientId5)
        .success
        .value shouldBe empty
    }

    "actualize loyalty report params and cashback for IN_PROGRESS" in {
      val periodId = PeriodId(1)
      val clientId1 = 21
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )
      loyaltyReportDao.insertItem(
        LoyaltyReportItem(
          reportId = id1,
          data = LoyaltyReportItemData(
            criterion = LoyaltyCriteria.ExtraBonus.toString,
            value = 0,
            resolution = false,
            comment = None,
            epoch = now()
          )
        )
      )
      val clientId2 = 22
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS_NEGATIVE
        )
      loyaltyReportDao.insertItem(
        LoyaltyReportItem(
          reportId = id2,
          data = LoyaltyReportItemData(
            criterion = LoyaltyCriteria.ExtraBonus.toString,
            value = 0,
            resolution = false,
            comment = None,
            epoch = now()
          )
        )
      )

      loyaltyReportDao
        .actualizeReports(
          NonEmptyList.of(
            LoyaltyReportActualization(
              id = id1,
              extrabonus = Some(ExtraBonus.OVER_2000_CARS),
              hasFullStock = true,
              cashbackPercent = 3,
              cashbackAmount = 150,
              placementDiscount = Some(15),
              updatedItems = List(
                LoyaltyReportItem(
                  reportId = id1,
                  data = LoyaltyReportItemData(
                    criterion = LoyaltyCriteria.ExtraBonus.toString,
                    value = 2,
                    resolution = true,
                    comment = None,
                    epoch = now()
                  )
                )
              )
            ),
            LoyaltyReportActualization(
              id = id2,
              extrabonus = None,
              hasFullStock = true,
              cashbackPercent = 4,
              cashbackAmount = 200,
              placementDiscount = None,
              updatedItems = List(
                LoyaltyReportItem(
                  reportId = id2,
                  data = LoyaltyReportItemData(
                    criterion = LoyaltyCriteria.ExtraBonus.toString,
                    value = 3,
                    resolution = true,
                    comment = None,
                    epoch = now()
                  )
                )
              )
            )
          )
        )
        .success
        .value

      val updated1 = loyaltyReportDao.find(periodId, clientId1)
      val updated2 = loyaltyReportDao.find(periodId, clientId2)

      updated1.status shouldBe "in_progress"
      updated1.extraBonus should contain(ExtraBonus.OVER_2000_CARS)
      updated1.hasFullStock should contain(true)
      updated1.cashbackPercent should contain(3)
      updated1.cashbackAmount should contain(150)
      updated1.placementDiscountPercent should contain(15)

      updated2.status shouldBe "in_progress_negative"
      updated2.extraBonus shouldBe empty
      updated2.hasFullStock should contain(true)
      updated2.cashbackPercent should contain(4)
      updated2.cashbackAmount should contain(200)
      updated2.placementDiscountPercent shouldBe empty

      val updatedItem1 =
        loyaltyReportDao.findItem(id1, LoyaltyCriteria.ExtraBonus)
      updatedItem1.data.value shouldBe 2
      updatedItem1.data.resolution shouldBe true

      val updatedItem2 =
        loyaltyReportDao.findItem(id2, LoyaltyCriteria.ExtraBonus)
      updatedItem2.data.value shouldBe 3
      updatedItem2.data.resolution shouldBe true
    }

    "actualize loyalty report params and cashback for PRE_APPROVED" in {
      val periodId = PeriodId(1)
      val clientId1 = 23
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )
      val clientId2 = 24
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )
      val clientId3 = 25
      val id3 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 200,
            cashbackPercent = 4,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )
      val clientId4 = 26
      val id4 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId4,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.PRE_APPROVED
        )

      loyaltyReportDao
        .actualizeReports(
          NonEmptyList.of(
            LoyaltyReportActualization(
              id = id1,
              extrabonus = Some(ExtraBonus.OVER_2000_CARS),
              hasFullStock = true,
              cashbackPercent = 3,
              cashbackAmount = 150,
              placementDiscount = Some(10),
              updatedItems = Nil
            ),
            LoyaltyReportActualization(
              id = id2,
              extrabonus = None,
              hasFullStock = true,
              cashbackPercent = 2,
              cashbackAmount = 100,
              placementDiscount = Some(0),
              updatedItems = Nil
            ),
            LoyaltyReportActualization(
              id = id3,
              extrabonus = None,
              hasFullStock = false,
              cashbackPercent = 4,
              cashbackAmount = 200,
              placementDiscount = Some(0),
              updatedItems = Nil
            ),
            LoyaltyReportActualization(
              id = id4,
              extrabonus = None,
              hasFullStock = false,
              cashbackPercent = 2,
              cashbackAmount = 100,
              placementDiscount = Some(25),
              updatedItems = Nil
            )
          )
        )
        .success
        .value

      val updated1 = loyaltyReportDao.find(periodId, clientId1)
      val updated2 = loyaltyReportDao.find(periodId, clientId2)
      val updated3 = loyaltyReportDao.find(periodId, clientId3)
      val updated4 = loyaltyReportDao.find(periodId, clientId4)

      updated1.status shouldBe "in_progress"
      updated1.extraBonus should contain(ExtraBonus.OVER_2000_CARS)
      updated1.hasFullStock should contain(true)
      updated1.cashbackPercent should contain(3)
      updated1.cashbackAmount should contain(150)
      updated1.placementDiscountPercent should contain(10)

      updated2.status shouldBe "in_progress"
      updated2.extraBonus shouldBe empty
      updated2.hasFullStock should contain(true)
      updated2.cashbackPercent should contain(2)
      updated2.cashbackAmount should contain(100)
      updated2.placementDiscountPercent should contain(0)

      updated3.status shouldBe "pre_approved"
      updated3.extraBonus shouldBe empty
      updated3.hasFullStock should contain(false)
      updated3.cashbackPercent should contain(4)
      updated3.cashbackAmount should contain(200)
      updated3.placementDiscountPercent should contain(0)

      updated4.status shouldBe "in_progress"
      updated4.extraBonus shouldBe empty
      updated4.hasFullStock should contain(false)
      updated4.cashbackPercent should contain(2)
      updated4.cashbackAmount should contain(100)
      updated4.placementDiscountPercent should contain(25)
    }

    "not actualize loyalty report params and cashback for APPROVED/APPLIED" in {
      val periodId = PeriodId(1)
      val clientId1 = 26
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )
      val clientId2 = 27
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPLIED
        )

      loyaltyReportDao
        .actualizeReports(
          NonEmptyList.of(
            LoyaltyReportActualization(
              id = id1,
              extrabonus = Some(ExtraBonus.OVER_2000_CARS),
              hasFullStock = true,
              cashbackPercent = 3,
              cashbackAmount = 150,
              placementDiscount = Some(20),
              updatedItems = Nil
            ),
            LoyaltyReportActualization(
              id = id2,
              extrabonus = None,
              hasFullStock = true,
              cashbackPercent = 4,
              cashbackAmount = 200,
              placementDiscount = Some(40),
              updatedItems = Nil
            )
          )
        )
        .success
        .value

      val updated1 = loyaltyReportDao.find(periodId, clientId1)
      val updated2 = loyaltyReportDao.find(periodId, clientId2)

      updated1.status shouldBe "approved"
      updated1.extraBonus shouldBe empty
      updated1.hasFullStock should contain(false)
      updated1.cashbackPercent should contain(2)
      updated1.cashbackAmount should contain(100)
      updated1.placementDiscountPercent should contain(0)

      updated2.status shouldBe "applied"
      updated2.extraBonus shouldBe empty
      updated2.hasFullStock should contain(false)
      updated2.cashbackPercent should contain(2)
      updated2.cashbackAmount should contain(100)
      updated2.placementDiscountPercent should contain(0)
    }

    "find active approved reports" in {
      val (activePeriodId, closedPeriodId) = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(10),
          DateTime.now().minusDays(1)
        )
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(20),
          DateTime.now().minusDays(11)
        )
        periods <- cashbackPeriodDao.getPeriods
        active = periods.head.id
        closed = periods(1).id
        _ <- cashbackPeriodDao.closeById(closed)
      } yield (active, closed)).success.value

      val approvedAt = DateTime.now();

      val clientId1 = 28
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None,
            vasSpendPercent = 10.some,
            placementSpendPercent = 90.some,
            resolution = true
          ),
          LoyaltyReportStatus.APPROVED,
          approvedAtOpt = Some(approvedAt)
        )

      // active period but not approved status
      val clientId2 = 29
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      // approved status but not active period
      val clientId3 = 30
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            closedPeriodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None,
            resolution = true
          ),
          LoyaltyReportStatus.APPROVED,
          approvedAtOpt = Some(approvedAt)
        )

      loyaltyReportDao.findActiveApproved.success.value should contain theSameElementsAs (List(
        ActiveApprovedReportInfo(
          id = id1,
          clientId = clientId1,
          periodId = activePeriodId,
          cashbackAmount = 100,
          proportions = CashbackProportions.of(10, 90).toOption.flatten,
          placementDiscountPercent = 0,
          placementDiscountOverride = None,
          resolution = true,
          approvedAt
        )
      ))
    }

    "find active negative reports" in {
      val (activePeriodId, closedPeriodId) = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(10),
          DateTime.now().minusDays(1)
        )
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(20),
          DateTime.now().minusDays(11)
        )
        periods <- cashbackPeriodDao.getPeriods
        active = periods.head.id
        closed = periods(1).id
        _ <- cashbackPeriodDao.closeById(closed)
      } yield (active, closed)).success.value

      val clientId1 = 31
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      // active period but positive status
      val clientId2 = 32
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      // negative status but not active period
      val clientId3 = 33
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            closedPeriodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      loyaltyReportDao.findActiveNegative.success.value should contain theSameElementsAs (List(
        ActiveNegativeReportInfo(id1, clientId1, activePeriodId)
      ))
    }

    "not return active negative reports if they've already been pushed" in {

      val activePeriodId = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(10),
          DateTime.now().minusDays(1)
        )
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(20),
          DateTime.now().minusDays(11)
        )
        periods <- cashbackPeriodDao.getPeriods
        active = periods.head.id
      } yield active).success.value

      val clientId1 = 31
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      val clientId2 = 32
      loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            activePeriodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED,
          negativeResolutionPushed = true
        )

      loyaltyReportDao.findActiveNegative.success.value should contain theSameElementsAs (List(
        ActiveNegativeReportInfo(id1, clientId1, activePeriodId)
      ))
    }

    "mark reports applied" in {
      val periodId = PeriodId(1)
      val clientId1 = 34
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      val clientId2 = 35
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      val clientId3 = 36
      val id3 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      loyaltyReportDao.markApplied(NonEmptyList.of(id1, id2, id3)).success.value

      loyaltyReportDao.find(periodId, clientId1).status shouldBe "applied"
      loyaltyReportDao.find(periodId, clientId2).status shouldBe "applied"
      loyaltyReportDao.find(periodId, clientId3).status shouldBe "in_progress"
    }

    "find current report and items" in {
      val periodId = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(2),
          DateTime.now().minusDays(1)
        )
        periods <- cashbackPeriodDao.getPeriods
      } yield periods.head.id).success.value

      val clientId = 37
      val report = InitialLoyaltyReport(
        periodId,
        clientId,
        loyaltyLevel = YearLoyaltyLevel,
        cashbackAmount = 100,
        cashbackPercent = 2,
        hasFullStock = false,
        extraBonus = None,
        placementDiscountPercent = 5
      )
      val id1 =
        loyaltyReportDao.plainInsert(report, LoyaltyReportStatus.IN_PROGRESS)

      val item1 =
        LoyaltyReportItem(
          reportId = id1,
          data = LoyaltyReportItemData(
            criterion = LoyaltyCriteria.ExtraBonus.toString,
            value = 0,
            resolution = false,
            comment = None,
            epoch = now()
          )
        )

      loyaltyReportDao.insertItem(item1)

      val item2 = LoyaltyReportItem(
        reportId = id1,
        data = LoyaltyReportItemData(
          criterion = LoyaltyCriteria.SiteCheck.toString,
          value = 0,
          resolution = false,
          comment = None,
          epoch = now()
        )
      )

      loyaltyReportDao.insertItem(item2)

      val res =
        loyaltyReportDao.findCurrentWithItems(clientId).success.value.map {
          case (report, items) => proto2initial(report) -> items.toSet
        }

      res should contain(report -> Set(item1, item2))

      loyaltyReportDao
        .findCurrent(clientId)
        .success
        .value
        .map(proto2initial) should contain(report)

      loyaltyReportDao.findCurrentWithItems(123).success.value shouldBe empty
    }

    "find previous report" in {
      val (currentPeriodId, previousPeriodId) = (for {
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(2),
          DateTime.now().minusDays(1)
        )
        _ <- cashbackPeriodDao.insert(
          DateTime.now().minusDays(4),
          DateTime.now().minusDays(3)
        )
        periods <- cashbackPeriodDao.getPeriods
      } yield
        periods match {
          case currentPeriod :: previousPeriod :: _ =>
            (currentPeriod.id, previousPeriod.id)
          case _ => fail("Not enough periods")
        }).success.value

      val clientId = 37
      val previousReport = InitialLoyaltyReport(
        previousPeriodId,
        clientId,
        loyaltyLevel = YearLoyaltyLevel,
        cashbackAmount = 100,
        cashbackPercent = 2,
        hasFullStock = false,
        extraBonus = None,
        placementDiscountPercent = 5
      )
      val currentReport = previousReport.copy(periodId = currentPeriodId)

      loyaltyReportDao.plainInsert(
        previousReport,
        LoyaltyReportStatus.IN_PROGRESS
      )
      loyaltyReportDao.plainInsert(
        currentReport,
        LoyaltyReportStatus.IN_PROGRESS
      )

      loyaltyReportDao
        .findPrevious(clientId)
        .success
        .value
        .map(proto2initial) should contain(previousReport)
    }

    "mark negative reports pushed" in {

      val periodId = PeriodId(1)
      val clientId1 = 34
      val id1 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId1,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.APPROVED
        )

      val clientId2 = 36
      val id2 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId2,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS
        )

      val clientId3 = 37
      val id3 = loyaltyReportDao
        .plainInsert(
          InitialLoyaltyReport(
            periodId,
            clientId3,
            loyaltyLevel = YearLoyaltyLevel,
            cashbackAmount = 100,
            cashbackPercent = 2,
            hasFullStock = false,
            extraBonus = None
          ),
          LoyaltyReportStatus.IN_PROGRESS_NEGATIVE
        )

      loyaltyReportDao
        .markNegativePushed(NonEmptyList.of(id1, id2, id3))
        .success
        .value

      loyaltyReportDao
        .find(periodId, clientId1)
        .negativeResolutionPushed shouldBe true
      loyaltyReportDao
        .find(periodId, clientId2)
        .negativeResolutionPushed shouldBe false
      loyaltyReportDao
        .find(periodId, clientId3)
        .negativeResolutionPushed shouldBe false
    }

    "not reset NEGATIVE_RESOLUTION_PUSHED when positive resolution comes" in {
      val (clientId, periodId) = (2L, PeriodId(2))
      val items = Nil

      val pushedReport =
        InitialLoyaltyReport(
          periodId,
          clientId,
          loyaltyLevel = YearLoyaltyLevel,
          cashbackAmount = 100,
          cashbackPercent = 2,
          hasFullStock = false,
          extraBonus = None
        )

      val updateReportMsg = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setResolution(true)
        .setCashbackAmount(10L)
        .setCashbackPercent(10)
        .setPeriodId(periodId)
        .setLoyaltyLevel(LoyaltyLevel.SmallestLoyalty.raw)
        .setActivationsAmount(100L)
        .setHasFullStock(true)
        .build()

      loyaltyReportDao
        .plainInsert(
          pushedReport,
          LoyaltyReportStatus.APPROVED,
          negativeResolutionPushed = true
        )

      val initial = loyaltyReportDao.find(periodId, clientId)

      initial.status shouldBe "approved"
      initial.negativeResolutionPushed shouldBe true

      val update = (for {
        _ <- loyaltyReportDao.upsert(updateReportMsg, items)
        updateResult = loyaltyReportDao.find(periodId, clientId)
      } yield updateResult).success.value

      update.negativeResolutionPushed shouldBe true
    }

    "find activations for report" in {
      val periodId = PeriodId(1)
      val clientId = 37

      val builder = LoyaltyReport
        .newBuilder()
        .setClientId(clientId)
        .setPeriodId(periodId)
        .setResolution(false)
        .setLoyaltyLevel(0)
        .setCashbackAmount(0)
        .setCashbackPercent(0)
        .setActivationsAmount(123)
        .setHasFullStock(false)

      loyaltyReportDao.upsert(builder.build(), Nil).success.value

      loyaltyReportDao
        .findActivationsAmount(clientId, periodId)
        .success
        .value shouldBe Some(123)
    }

    "return none if no activations amount" in {
      loyaltyReportDao
        .findActivationsAmount(666, PeriodId(14))
        .success
        .value shouldBe None
    }
  }

  trait TestLoyaltyReportDaoExtensions extends LoyaltyReportDao {

    def plainInsert(
        initial: InitialLoyaltyReport,
        status: LoyaltyReportStatus,
        negativeResolutionPushed: Boolean = false,
        managerName: String = "",
        approvedAtOpt: Option[DateTime] = None
    ): Long

    def find(periodId: PeriodId, clientId: ClientId): LoyaltyReportTest

    def insertItem(item: LoyaltyReportItem): Unit

    def findItem(reportId: Long, criterion: LoyaltyCriteria): LoyaltyReportItem

    def findItems(reportId: Long): List[LoyaltyReportItem]
  }

  case class LoyaltyReportTest(
      id: Long,
      periodId: PeriodId,
      clientId: ClientId,
      status: String,
      preApprovedBy: Option[String] = None,
      preApprovedDate: Option[DateTime] = None,
      approvedBy: Option[String] = None,
      approvedDate: Option[DateTime] = None,
      extraBonus: Option[ExtraBonus] = None,
      hasFullStock: Option[Boolean] = None,
      cashbackPercent: Option[Int] = None,
      cashbackAmount: Option[Funds] = None,
      negativeResolutionPushed: Boolean = false,
      placementDiscountPercent: Option[Int] = None,
      revokedBy: Option[String] = None,
      revokedDate: Option[DateTime] = None,
      placementDiscountOverride: Option[PlacementDiscountOverride] = None
  )

  case class PlacementDiscountOverride(
      newDiscount: Int,
      editor: String,
      comment: String,
      editDate: DateTime
  )

  object PlacementDiscountOverride {

    def fromOptions(
        newDiscount: Option[Int],
        editor: Option[String],
        comment: Option[String],
        editDate: Option[DateTime]
    ): Option[PlacementDiscountOverride] = {

      val fields = List(newDiscount, editor, comment, editDate)
      if (fields.exists(_.nonEmpty) && fields.exists(_.isEmpty))
        throw new AssertionError(
          "All fields for placement discount override should be set, or none of them should be set."
        )

      if (fields.forall(_.nonEmpty))
        Some(
          PlacementDiscountOverride(
            newDiscount.get,
            editor.get,
            comment.get,
            editDate.get
          )
        )
      else None
    }
  }

  case class InitialLoyaltyReport(
      periodId: PeriodId,
      clientId: ClientId,
      loyaltyLevel: LoyaltyLevel,
      cashbackAmount: Funds,
      cashbackPercent: Percent,
      hasFullStock: Boolean,
      extraBonus: Option[ExtraBonus],
      vasSpendPercent: Option[Percent] = None,
      placementSpendPercent: Option[Percent] = None,
      placementDiscountPercent: Int = 0,
      resolution: Boolean = false
  )

  "get reports by periodID" in {
    val report1ForFirstPeriod = InitialLoyaltyReport(
      periodId = PeriodId(1),
      clientId = 1,
      loyaltyLevel = YearLoyaltyLevel,
      cashbackAmount = 1,
      cashbackPercent = 1,
      hasFullStock = true,
      extraBonus = None
    )

    val report2ForFirstPeriod = InitialLoyaltyReport(
      periodId = PeriodId(1),
      clientId = 2,
      loyaltyLevel = HalfYearLoyaltyLevel,
      cashbackAmount = 3,
      cashbackPercent = 6,
      hasFullStock = false,
      extraBonus = Some(ExtraBonus.OVER_2000_CARS)
    )

    val report1ForSecondPeriod = InitialLoyaltyReport(
      periodId = PeriodId(2),
      clientId = 1,
      loyaltyLevel = HalfYearLoyaltyLevel,
      cashbackAmount = 10,
      cashbackPercent = 1,
      hasFullStock = false,
      extraBonus = Some(ExtraBonus.OVER_2000_CARS)
    )
    val z = for {
      _ <- ZIO(
        loyaltyReportDao
          .plainInsert(report1ForFirstPeriod, LoyaltyReportStatus.APPLIED)
      )
      _ <- ZIO(
        loyaltyReportDao
          .plainInsert(report2ForFirstPeriod, LoyaltyReportStatus.APPLIED)
      )
      _ <- ZIO(
        loyaltyReportDao
          .plainInsert(report1ForSecondPeriod, LoyaltyReportStatus.APPLIED)
      )
      firstPeriodReports <- loyaltyReportDao.findByPeriodID(PeriodId(1))
    } yield {
      firstPeriodReports.size should be(2)
      firstPeriodReports.map(
        proto2initial
      ) should contain allOf (report1ForFirstPeriod, report2ForFirstPeriod)
    }

    z.success.value
  }

  "When loyalty report comment operation is requested" should {
    def addInitialData(): Unit = {
      val report1ForFirstPeriod = InitialLoyaltyReport(
        periodId = PeriodId(1),
        clientId = 1,
        loyaltyLevel = YearLoyaltyLevel,
        cashbackAmount = 1,
        cashbackPercent = 1,
        hasFullStock = true,
        extraBonus = None
      )
      loyaltyReportDao.plainInsert(
        report1ForFirstPeriod,
        LoyaltyReportStatus.APPLIED
      )
    }

    "successfully insert new comment for existing period" in {
      addInitialData()
      loyaltyReportDao
        .addComment(PeriodId(1), 1L, "test comment", StaffId("Unit Tester"))
        .success
        .value shouldBe Updated
    }

    "fail if comment exists" in {
      addInitialData()
      loyaltyReportDao
        .addComment(PeriodId(1), 1L, "test comment", StaffId("Unit Tester"))
        .success
        .value
      loyaltyReportDao
        .addComment(PeriodId(1), 1L, "test comment 2", StaffId("Unit Tester"))
        .success
        .value shouldBe CommentAlreadyExisted
    }

    "fail if period not found" in {
      addInitialData()
      loyaltyReportDao
        .addComment(PeriodId(2), 1L, "test comment", StaffId("Unit Tester"))
        .success
        .value shouldBe LoyaltyReportNotFound
    }

    "fail if client not found" in {
      addInitialData()
      loyaltyReportDao
        .addComment(PeriodId(1), 2L, "test comment", StaffId("Unit Tester"))
        .success
        .value shouldBe LoyaltyReportNotFound
    }

    "successfully delete existing comment" in {
      addInitialData()
      loyaltyReportDao
        .addComment(PeriodId(1), 1L, "test comment", StaffId("Unit Tester"))
        .success
        .value
      loyaltyReportDao
        .deleteComment(PeriodId(1), 1L, StaffId("Unit Tester"))
        .success
        .value shouldBe true
    }

    "do not fail on delete if comment doesn't exist" in {
      addInitialData()
      loyaltyReportDao
        .deleteComment(PeriodId(1), 1L, StaffId("Unit Tester"))
        .success
        .value shouldBe true
    }

    "fail to delete if period is not found" in {
      addInitialData()
      loyaltyReportDao
        .deleteComment(PeriodId(2), 1L, StaffId("Unit Tester"))
        .success
        .value shouldBe false
    }

    "fail to delete if client is not found" in {
      addInitialData()
      loyaltyReportDao
        .deleteComment(PeriodId(1), 2L, StaffId("Unit Tester"))
        .success
        .value shouldBe false
    }
  }

  "revoke" should {
    "mark report as IN_PROGRESS, set user and timestamp" in {
      val userId = StaffId("Unit Tester")
      val clientId = 1L
      val periodId = PeriodId(1)

      loyaltyReportDao.plainInsert(
        InitialLoyaltyReport(
          periodId = periodId,
          clientId = clientId,
          loyaltyLevel = YearLoyaltyLevel,
          cashbackAmount = 1,
          cashbackPercent = 1,
          hasFullStock = true,
          extraBonus = None
        ),
        LoyaltyReportStatus.APPLIED
      )

      loyaltyReportDao
        .revoke(clientId, periodId, userId)
        .success
        .value

      val updated = loyaltyReportDao.find(periodId, clientId)

      updated.status shouldBe "in_progress"
      updated.revokedBy.value shouldBe userId
      updated.revokedDate should not be empty
    }
  }

  "edit placement discount" should {
    "set discount override" in {
      val reportId = 1L
      val clientId = 2L
      val periodId = PeriodId(3)
      val oldDiscount = 20
      val newDiscount = 40
      val editor = StaffId("Unittest editor")
      val comment = "testing"
      val report = LoyaltyReport
        .newBuilder()
        .setId(reportId)
        .setClientId(clientId)
        .setPeriodId(periodId)
        .setPlacementDiscountPercent(oldDiscount)
        .build()

      // иначе странные результаты от сравнения времени из разных сред
      val testStartTime = databaseFunctionsDao.now()
      loyaltyReportDao.upsert(report, Nil).success.value
      val result =
        loyaltyReportDao
          .editPlacementDiscount(reportId, newDiscount, editor, comment)
          .success
          .value
      result shouldBe true

      val updated = loyaltyReportDao.find(periodId, clientId)
      updated.id shouldBe reportId
      updated.placementDiscountPercent shouldBe oldDiscount.some
      updated.placementDiscountOverride should not be none
      val discountOverride = updated.placementDiscountOverride.get
      discountOverride.editor shouldBe editor
      discountOverride.comment shouldBe comment
      discountOverride.editDate should be >= testStartTime
    }
    "set discount override twice" in {
      val reportId = 1L
      val clientId = 2L
      val periodId = PeriodId(3)
      val oldDiscount = 20
      val newDiscountA = 40
      val editorA = StaffId("Unittest editor A")
      val commentA = "testing"
      val newDiscountB = 60
      val editorB = StaffId("Unittest editor B")
      val commentB = "testing B"
      val report = LoyaltyReport
        .newBuilder()
        .setId(reportId)
        .setClientId(clientId)
        .setPeriodId(periodId)
        .setPlacementDiscountPercent(oldDiscount)
        .build()

      loyaltyReportDao.upsert(report, Nil).success.value
      val resultA =
        loyaltyReportDao
          .editPlacementDiscount(reportId, newDiscountA, editorA, commentA)
          .success
          .value
      resultA shouldBe true

      // иначе странные результаты от сравнения времени из разных сред
      val timeBeforeEditB = databaseFunctionsDao.now()
      val resultB =
        loyaltyReportDao
          .editPlacementDiscount(reportId, newDiscountB, editorB, commentB)
          .success
          .value
      resultB shouldBe true

      val updated = loyaltyReportDao.find(periodId, clientId)
      updated.id shouldBe reportId
      updated.placementDiscountPercent shouldBe oldDiscount.some
      updated.placementDiscountOverride should not be None
      val discountOverride = updated.placementDiscountOverride.get
      discountOverride.editor shouldBe editorB
      discountOverride.comment shouldBe commentB
      discountOverride.editDate should be >= timeBeforeEditB
    }
    "error if report id doesn't exist" in {
      val reportId = 1L
      val wrongReportId = 120L
      val clientId = 2L
      val periodId = PeriodId(3)
      val oldDiscount = 20
      val newDiscount = 40
      val editor = StaffId("Unittest editor")
      val comment = "testing"
      val report = LoyaltyReport
        .newBuilder()
        .setId(reportId)
        .setClientId(clientId)
        .setPeriodId(periodId)
        .setPlacementDiscountPercent(oldDiscount)
        .build()

      loyaltyReportDao.upsert(report, Nil).success.value
      val result =
        loyaltyReportDao
          .editPlacementDiscount(wrongReportId, newDiscount, editor, comment)
          .success
          .value
      result shouldBe false

      val notUpdated = loyaltyReportDao.find(periodId, clientId)
      notUpdated.id shouldBe reportId
      notUpdated.placementDiscountPercent shouldBe oldDiscount.some
      notUpdated.placementDiscountOverride shouldBe None
    }
  }

  private def proto2initial(report: LoyaltyReport): InitialLoyaltyReport =
    InitialLoyaltyReport(
      PeriodId(report.getPeriodId),
      report.getClientId,
      LoyaltyLevel(report.getLoyaltyLevel).get,
      report.getCashbackAmount,
      report.getCashbackPercent,
      report.getHasFullStock,
      Option(report.getExtraBonus).filter(_ != ExtraBonus.UNKNOWN_BONUS),
      vasSpendPercent = None,
      placementSpendPercent = None,
      placementDiscountPercent = report.getPlacementDiscountPercent
    )

  private def withItems[T](test: List[LoyaltyReportItemData] => T): T = {
    val items = List(
      LoyaltyReportItemData("inactivity-90-days-period", 90, true, None, now),
      LoyaltyReportItemData("inactivity-182-days-period", 182, true, None, now),
      LoyaltyReportItemData("inactivity-365-days-period", 365, true, None, now),
      LoyaltyReportItemData(
        "banned-offers",
        0,
        true,
        Some(
          "[Превышение порога по замороженным объявлениям]процент замороженных: 0.0,порог: 5"
        ),
        now
      ),
      LoyaltyReportItemData(
        "site-check",
        1,
        false,
        Some("[Последняя проверка не пройдена]"),
        now
      ),
      LoyaltyReportItemData(
        "exclusivity",
        70,
        true,
        Some(
          "[Доля эксклюзивных офферов больше пороговой]\nпроцент эксклюзивных: 70\nпорог: 60"
        ),
        now
      ),
      LoyaltyReportItemData(
        "full_stock",
        1,
        true,
        Some("Склады заполнены"),
        now
      ),
      LoyaltyReportItemData(
        "extra_bonus",
        0,
        false,
        Some("Нет дополнительного кешбэка за большое количество автомобилей"),
        now
      )
    )
    test(items)
  }
}
