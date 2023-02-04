package auto.dealers.loyalty.storage.jdbc

import auto.dealers.loyalty.model.ClientChangedBufferRecord.InputRecord
import auto.dealers.loyalty.model.LoyaltyLevel.YearLoyaltyLevel
import auto.dealers.loyalty.model._
import auto.dealers.loyalty.storage.ClientsChangedBufferDao.DataSourceFilter
import auto.dealers.loyalty.storage.LoyaltyReportDao._
import cats.data.NonEmptyList
import com.google.protobuf.util.Timestamps
import common.zio.doobie.schema.InitSchema
import common.zio.doobie.testkit.TestMySQL
import doobie.Transactor
import doobie.implicits._
import doobie.util.fragment.Fragment
import ru.auto.cabinet.ApiModel.ExtraBonus
import ru.auto.salesman.model.cashback.ApiModel.{ClientCashbackInfo, LoyaltyReport, LoyaltyReportStatus}
import zio.{IO, Task, ZIO}
import zio.interop.catz._
import zio.test.{DefaultRunnableSpec, ZSpec, _}
import zio.test.Assertion._
import zio.test.TestAspect.{beforeAll, _}

import java.time.{Instant, LocalDate}
import java.time.temporal.ChronoUnit

object JdbcLoyaltyReportDaoSpec extends DefaultRunnableSpec {

  import JdbcLoyaltyReportDao._

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
      resolution: Boolean = false)

  object InitialLoyaltyReport {

    def fromLoyaltyReport(report: LoyaltyReport): InitialLoyaltyReport =
      InitialLoyaltyReport(
        periodId = report.getPeriodId,
        clientId = report.getClientId,
        loyaltyLevel = LoyaltyLevel.unsafeApply(report.getLoyaltyLevel),
        cashbackAmount = report.getCashbackAmount,
        cashbackPercent = report.getCashbackPercent,
        hasFullStock = report.getHasFullStock,
        extraBonus = Option(report.getExtraBonus).filterNot(_ == ExtraBonus.UNKNOWN_BONUS),
        vasSpendPercent = None,
        placementSpendPercent = None,
        placementDiscountPercent = report.getPlacementDiscountPercent
      )
  }

  private def createBaseInitialReport(periodId: PeriodId, clientId: ClientId): InitialLoyaltyReport =
    InitialLoyaltyReport(
      periodId,
      clientId,
      loyaltyLevel = YearLoyaltyLevel,
      cashbackAmount = 100,
      cashbackPercent = 2,
      hasFullStock = false,
      extraBonus = None
    )

  private def createBaseReportItem(
      reportId: Long,
      value: Int = 0,
      resolution: Boolean = false,
      criterion: LoyaltyCriteria = LoyaltyCriteria.ExtraBonus): LoyaltyReportItem =
    LoyaltyReportItem(
      reportId = reportId,
      data = LoyaltyReportItemData(
        criterion = criterion.toString,
        value = value,
        resolution = resolution,
        comment = None,
        epoch = Instant.now().truncatedTo(ChronoUnit.MILLIS)
      )
    )

  private def createClientCashbackInfo(clientId: ClientId, amount: Int = 100): ClientCashbackInfo =
    ClientCashbackInfo
      .newBuilder()
      .setClientId(clientId)
      .setCashbackAmount(amount)
      .build()

  private def insertLoyaltyReport(
      report: InitialLoyaltyReport,
      status: LoyaltyReportStatus,
      negativeResolutionPushed: Boolean = false,
      managerName: String = ""
    )(xa: Transactor[Task]): Task[Unit] =
    sql"""
      INSERT INTO loyalty_report (
        `period_id`,
        `client_id`,
        `loyalty_level`,
        `cashback_amount`,
        `cashback_percent`,
        `extra_bonus`,
        `has_full_stock`,
        `resolution`,
        `activations_amount`,
        `status`,
        `negative_resolution_pushed`,
        `manager_name`,
        `vas_spend_percent`,
        `placement_spend_percent`,
        `placement_discount_percent`
      ) VALUES (
        ${report.periodId},
        ${report.clientId},
        ${report.loyaltyLevel.raw},
        ${report.cashbackAmount},
        ${report.cashbackPercent},
        ${report.extraBonus},
        ${report.hasFullStock},
        ${report.resolution},
        0,
        ${status.name()},
        $negativeResolutionPushed,
        $managerName,
        ${report.vasSpendPercent},
        ${report.placementSpendPercent},
        ${report.placementDiscountPercent})
      """.update.run
      .transact(xa)
      .unit

  def insertLoyaltyReportItem(item: LoyaltyReportItem)(xa: Transactor[Task]): Task[Unit] =
    sql"""
      INSERT INTO loyalty_report_item (
        `report_id`,
        `criterion`,
        `value`,
        `resolution`,
        `comment`,
        `epoch`
      ) VALUES(
        ${item.reportId},
        ${item.data.criterion},
        ${item.data.value},
        ${item.data.resolution},
        ${item.data.comment},
        ${item.data.epoch})
      """.update.run
      .transact(xa)
      .unit

  private val loyaltyReportIdsSelector: Fragment = fr"SELECT id FROM loyalty_report"

  private def selectLoyaltyReportIds(xa: Transactor[Task]): Task[List[Long]] =
    loyaltyReportIdsSelector.query[Long].to[List].map(_.sorted).transact(xa)

  private def selectExactLoyaltyReportId(
      reportPeriodId: PeriodId,
      reportClientId: ClientId
    )(xa: Transactor[Task]): IO[Throwable, Long] =
    sql"$loyaltyReportIdsSelector WHERE period_id=$reportPeriodId AND client_id=$reportClientId"
      .query[Long]
      .to[List]
      .transact(xa)
      .map(_.head)

  private val periodId: PeriodId = 1L
  private val secondPeriodId: PeriodId = 2L

  private val clientId: ClientId = 1L
  private val secondClientId: ClientId = 2L
  private val thirdClientId: ClientId = 3L

  private val approvingUser = "test_user"

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] = {
    (suite("JdbcLoyaltyReportDao")(
      suite("Base dao methods")(
        testM("not set pre_approve on report with status=applied") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstClientReport = createBaseInitialReport(periodId, clientId)
            secondClientReport = createBaseInitialReport(periodId, secondClientId)

            _ <- insertLoyaltyReport(firstClientReport, LoyaltyReportStatus.APPLIED)(xa)
            _ <- insertLoyaltyReport(secondClientReport, LoyaltyReportStatus.APPLIED)(xa)

            reportsBeforeApprove <- loyaltyReportClient.findByPeriodID(periodId)

            toPreApprove = NonEmptyList.of(createClientCashbackInfo(clientId), createClientCashbackInfo(secondClientId))
            notApprovedReports <- loyaltyReportClient.setPreApprove(approvingUser, periodId, toPreApprove)

            reportsAfterApprove <- loyaltyReportClient.findByPeriodID(periodId)
          } yield assert(reportsBeforeApprove.map(_.getStatus))(forall(equalTo(LoyaltyReportStatus.APPLIED))) &&
            assert(reportsAfterApprove.map(_.getStatus))(forall(equalTo(LoyaltyReportStatus.APPLIED))) &&
            assert(notApprovedReports)(
              hasSameElements(
                List(
                  NotApprovedReportInfo(clientId, LoyaltyReportStatus.APPLIED, 100),
                  NotApprovedReportInfo(secondClientId, LoyaltyReportStatus.APPLIED, 100)
                )
              )
            )
        },
        testM("not set pre_approve on report pre_approved by other user") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)

            toPreApprove = NonEmptyList.of(createClientCashbackInfo(clientId))
            notApprovedReports <- loyaltyReportClient.setPreApprove(approvingUser, periodId, toPreApprove)
            secondaryPreApprovingUser = "test_user2"
            secondaryNotApprovedReports <- loyaltyReportClient
              .setPreApprove(secondaryPreApprovingUser, periodId, toPreApprove)

            reports <- loyaltyReportClient.findByPeriodID(periodId)
            reportChecks = assert(reports)(hasSize(equalTo(1))) &&
              assert(reports.head.getClientId)(equalTo(clientId)) &&
              assert(reports.head.getStatus)(equalTo(LoyaltyReportStatus.PRE_APPROVED)) &&
              assertTrue(approvingUser != secondaryPreApprovingUser) &&
              assert(reports.head.getApprovals.getPreApprovedBy)(equalTo(approvingUser)) &&
              assert(reports.head.getApprovals.getPreApprovedDate.getSeconds)(isGreaterThan(0L))
          } yield reportChecks &&
            assert(notApprovedReports)(isEmpty) &&
            assert(secondaryNotApprovedReports)(
              equalTo(List(NotApprovedReportInfo(clientId, LoyaltyReportStatus.PRE_APPROVED, 100)))
            )
        },
        testM("set pre_approve on report with status = in_progress") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            secondReport = createBaseInitialReport(periodId, secondClientId)

            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            _ <- insertLoyaltyReport(secondReport, LoyaltyReportStatus.APPROVED)(xa)

            toPreApprove = NonEmptyList.of(createClientCashbackInfo(clientId), createClientCashbackInfo(secondClientId))
            notApprovedReports <- loyaltyReportClient.setPreApprove(approvingUser, periodId, toPreApprove)

            reports <- loyaltyReportClient.findByPeriodID(periodId)
            (firstClientReport, secondClientReport) = reports.partition(_.getClientId == clientId)

            firstClientReportChecks = assert(firstClientReport)(hasSize(equalTo(1))) &&
              assert(firstClientReport.head.getStatus)(equalTo(LoyaltyReportStatus.PRE_APPROVED)) &&
              assert(firstClientReport.head.getApprovals.getPreApprovedBy)(equalTo(approvingUser)) &&
              assert(firstClientReport.head.getApprovals.getPreApprovedDate.getSeconds)(isGreaterThan(0L))
          } yield firstClientReportChecks &&
            assert(secondClientReport.map(_.getStatus))(equalTo(List(LoyaltyReportStatus.APPROVED))) &&
            assert(notApprovedReports)(
              equalTo(List(NotApprovedReportInfo(secondClientId, LoyaltyReportStatus.APPROVED, 100)))
            )
        },
        testM("remove pre approve") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            secondReport = createBaseInitialReport(periodId, secondClientId)
            thirdReport = createBaseInitialReport(periodId, thirdClientId)

            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.PRE_APPROVED)(xa)
            _ <- insertLoyaltyReport(secondReport, LoyaltyReportStatus.APPROVED)(xa)
            _ <- insertLoyaltyReport(thirdReport, LoyaltyReportStatus.PRE_APPROVED)(xa)

            toRemovePreApprove = NonEmptyList.of(
              createClientCashbackInfo(clientId),
              createClientCashbackInfo(secondClientId),
              createClientCashbackInfo(thirdClientId, amount = 10)
            )
            notApprovedReports <- loyaltyReportClient.removePreApprove(periodId, toRemovePreApprove)

            reports <- loyaltyReportClient.findByPeriodID(periodId).map(_.groupBy(_.getClientId))
            firstClientReportChecks = assert(reports(clientId))(hasSize(equalTo(1))) &&
              assert(reports(clientId).head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(reports(clientId).head.getApprovals.getPreApprovedBy)(isEmptyString) &&
              assert(reports(clientId).head.getApprovals.getPreApprovedDate.getSeconds)(equalTo(0L))
          } yield firstClientReportChecks &&
            assert(notApprovedReports)(
              hasSameElements(
                List(
                  NotApprovedReportInfo(secondClientId, LoyaltyReportStatus.APPROVED, 100),
                  NotApprovedReportInfo(thirdClientId, LoyaltyReportStatus.PRE_APPROVED, 100)
                )
              )
            ) &&
            assert(reports.get(secondClientId).toList.flatten.map(_.getStatus))(
              equalTo(List(LoyaltyReportStatus.APPROVED))
            ) &&
            assert(reports.get(thirdClientId).toList.flatten.map(_.getStatus))(
              equalTo(List(LoyaltyReportStatus.PRE_APPROVED))
            )
        },
        testM("update existing record") {
          val currentTimeMillis = Instant.now().truncatedTo(ChronoUnit.MILLIS)
          val items = List(
            LoyaltyReportItemData("inactivity-90-days-period", 90, true, None, currentTimeMillis),
            LoyaltyReportItemData("inactivity-182-days-period", 182, true, None, currentTimeMillis),
            LoyaltyReportItemData("inactivity-365-days-period", 365, true, None, currentTimeMillis),
            LoyaltyReportItemData(
              "banned-offers",
              0,
              true,
              Some("[Превышение порога по замороженным объявлениям]процент замороженных: 0.0,порог: 5"),
              currentTimeMillis
            ),
            LoyaltyReportItemData(
              "site-check",
              1,
              false,
              Some("[Последняя проверка не пройдена]"),
              currentTimeMillis
            ),
            LoyaltyReportItemData(
              "exclusivity",
              70,
              true,
              Some("[Доля эксклюзивных офферов больше пороговой]\nпроцент эксклюзивных: 70\nпорог: 60"),
              currentTimeMillis
            ),
            LoyaltyReportItemData(
              "full_stock",
              1,
              true,
              Some("Склады заполнены"),
              currentTimeMillis
            ),
            LoyaltyReportItemData(
              "extra_bonus",
              0,
              false,
              Some("Нет дополнительного кешбэка за большое количество автомобилей"),
              currentTimeMillis
            )
          )

          // report from kafka
          val initialReportMsg = LoyaltyReport
            .newBuilder()
            .setClientId(clientId)
            .setResolution(false)
            .setCashbackAmount(0L)
            .setCashbackPercent(0)
            .setOverrideUntil(Timestamps.fromMillis(currentTimeMillis.toEpochMilli))
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

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- loyaltyReportClient.upsert(initialReportMsg, items)
            insertResult <- loyaltyReportClient.findByPeriodID(periodId)

            // insertion with same resolution should not affect clients_changed_buffer
            _ <- loyaltyReportClient.upsert(initialReportMsg, items)
            _ <- loyaltyReportClient.upsert(updateReportMsg, items)
            _ <- loyaltyReportClient.upsert(updateReportMsg, items)

            updateResult <- loyaltyReportClient.findByPeriodID(periodId)
            insertedItems <-
              sql"""
                SELECT `criterion`, `value`, `resolution`, `comment`, `epoch`, `report_id`
                FROM loyalty_report_item
                WHERE report_id=${insertResult.head.getId}
                """
                .query[LoyaltyReportItem]
                .to[List]
                .transact(xa)

            insertResultChecks = assert(insertResult)(hasSize(equalTo(1))) &&
              assert(insertResult.head.getActivationsAmount)(equalTo(0L)) &&
              assert(insertResult.head.getResolution)(equalTo(false)) &&
              assert(insertResult.head.getLoyaltyLevel)(equalTo(LoyaltyLevel.NoLoyalty.raw)) &&
              assert(insertResult.head.getCashbackAmount)(equalTo(0L)) &&
              assert(insertResult.head.getCashbackPercent)(equalTo(0)) &&
              assert(insertResult.head.getExtraBonus)(equalTo(ExtraBonus.OVER_2000_CARS)) &&
              assert(insertResult.head.getHasFullStock)(equalTo(false)) &&
              assert(insertResult.head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS_NEGATIVE)) &&
              assert(insertResult.head.hasOverrideUntil)(equalTo(true)) &&
              assert(insertResult.head.getManagerName)(equalTo("")) &&
              assert(insertResult.head.getVasSpendPercent)(equalTo(0)) &&
              assert(insertResult.head.getPlacementDiscountPercent)(equalTo(0)) &&
              assert(insertResult.head.getPlacementSpendPercent)(equalTo(0)) &&
              assert(Timestamps.toMillis(insertResult.head.getOverrideUntil))(equalTo(currentTimeMillis.toEpochMilli))

            updateResultChecks = assert(updateResult)(hasSize(equalTo(1))) &&
              assert(updateResult.head.getActivationsAmount)(equalTo(100L)) &&
              assert(updateResult.head.getResolution)(equalTo(true)) &&
              assert(updateResult.head.getLoyaltyLevel)(equalTo(LoyaltyLevel.SmallestLoyalty.raw)) &&
              assert(updateResult.head.getCashbackAmount)(equalTo(10L)) &&
              assert(updateResult.head.getCashbackPercent)(equalTo(10)) &&
              assert(updateResult.head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updateResult.head.getHasFullStock)(equalTo(true)) &&
              assert(updateResult.head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(updateResult.head.hasOverrideUntil)(equalTo(false)) &&
              assert(updateResult.head.getManagerName)(equalTo("updated")) &&
              assert(updateResult.head.getVasSpendPercent)(equalTo(1)) &&
              assert(updateResult.head.getPlacementSpendPercent)(equalTo(2)) &&
              assert(updateResult.head.getPlacementDiscountPercent)(equalTo(20)) &&
              assert(updateResult.head.getAutoruExclusivePercent)(equalTo(10))

            expectedItems = items.map { data =>
              LoyaltyReportItem(reportId = insertResult.head.getId, data = data)
            }
          } yield insertResultChecks && updateResultChecks && assert(insertedItems)(hasSameElements(expectedItems))
        },
        testM("not update approved report") {
          val initialReport = createBaseInitialReport(periodId, clientId)

          val updateReportMsg = LoyaltyReport
            .newBuilder()
            .setClientId(clientId)
            .setResolution(false)
            .setOverrideUntil(Timestamps.fromMillis(Instant.now().toEpochMilli))
            .setCashbackAmount(20L)
            .setCashbackPercent(20)
            .setPeriodId(periodId)
            .setLoyaltyLevel(LoyaltyLevel.SmallestLoyalty.raw)
            .setActivationsAmount(100)
            .setExtraBonus(ExtraBonus.UNDER_2000_CARS)
            .setHasFullStock(true)
            .setManagerName("updated")
            .build()

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- insertLoyaltyReport(initialReport, LoyaltyReportStatus.APPLIED)(xa)

            insertResult <- loyaltyReportClient.findByPeriodID(periodId)
            _ <- loyaltyReportClient.upsert(updateReportMsg, items = Nil)
            updateResult <- loyaltyReportClient.findByPeriodID(periodId)
          } yield assert(insertResult)(hasSameElements(updateResult))
        },
        testM("not set approve on report with status=applied") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            secondReport = createBaseInitialReport(periodId, secondClientId)

            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.APPLIED)(xa)
            _ <- insertLoyaltyReport(secondReport, LoyaltyReportStatus.APPLIED)(xa)

            reportsBeforeApprove <- loyaltyReportClient.findByPeriodID(periodId)

            toApprove = NonEmptyList.of(createClientCashbackInfo(clientId), createClientCashbackInfo(secondClientId))
            notApprovedReports <- loyaltyReportClient.setApprove(approvingUser, periodId, toApprove)

            reportsAfterApprove <- loyaltyReportClient.findByPeriodID(periodId)
          } yield assert(reportsBeforeApprove.map(_.getStatus))(forall(equalTo(LoyaltyReportStatus.APPLIED))) &&
            assert(reportsAfterApprove.map(_.getStatus))(forall(equalTo(LoyaltyReportStatus.APPLIED))) &&
            assert(notApprovedReports)(
              hasSameElements(
                List(
                  NotApprovedReportInfo(clientId, LoyaltyReportStatus.APPLIED, 100),
                  NotApprovedReportInfo(secondClientId, LoyaltyReportStatus.APPLIED, 100)
                )
              )
            )
        },
        testM("not set approve on report approved by other user") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)

            toApprove = NonEmptyList.of(createClientCashbackInfo(clientId))
            notApprovedReports <- loyaltyReportClient.setApprove(approvingUser, periodId, toApprove)
            secondaryApprovingUser = "test_user2"
            secondaryNotApprovedReports <- loyaltyReportClient.setApprove(secondaryApprovingUser, periodId, toApprove)

            report <- loyaltyReportClient.findByPeriodID(periodId)
            resultReportChecks = assert(report)(hasSize(equalTo(1))) &&
              assert(report.head.getClientId)(equalTo(clientId)) &&
              assert(report.head.getStatus)(equalTo(LoyaltyReportStatus.APPROVED)) &&
              assertTrue(approvingUser != secondaryApprovingUser) &&
              assert(report.head.getApprovals.getApprovedBy)(equalTo(approvingUser)) &&
              assert(report.head.getApprovals.getApprovedDate.getSeconds)(isGreaterThan(0L))
          } yield resultReportChecks &&
            assert(notApprovedReports)(equalTo(Nil)) &&
            assert(secondaryNotApprovedReports)(
              equalTo(List(NotApprovedReportInfo(clientId, LoyaltyReportStatus.APPROVED, 100)))
            )
        },
        testM("set approve on report with status = in_progress") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)
            clientsChangedBufferClient = new JdbcClientsChangedBufferDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)
            secondReport = createBaseInitialReport(periodId, secondClientId)

            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            _ <- insertLoyaltyReport(secondReport, LoyaltyReportStatus.APPLIED)(xa)

            toApprove = NonEmptyList.of(createClientCashbackInfo(clientId), createClientCashbackInfo(secondClientId))
            notApprovedReports <- loyaltyReportClient.setApprove(approvingUser, periodId, toApprove)

            reportResult <- loyaltyReportClient.findByPeriodID(periodId).map(_.groupBy(_.getClientId))
            firstClientResultChecks = assert(reportResult(clientId))(hasSize(equalTo(1))) &&
              assert(reportResult(clientId).head.getStatus)(equalTo(LoyaltyReportStatus.APPROVED)) &&
              assert(reportResult(clientId).head.getApprovals.getApprovedBy)(equalTo(approvingUser)) &&
              assert(reportResult(clientId).head.getApprovals.getApprovedDate.getSeconds)(isGreaterThan(0L))

            clientsChangedBufferResult <-
              clientsChangedBufferClient
                .get(DataSourceFilter(Set("loyalty_report", "dealer_pony")))
                .map(resultRecords => resultRecords.map(record => InputRecord(record.clientId, record.dataSource)))
          } yield firstClientResultChecks &&
            assert(notApprovedReports)(
              equalTo(List(NotApprovedReportInfo(secondClientId, LoyaltyReportStatus.APPLIED, 100)))
            ) &&
            assert(reportResult(secondClientId).head.getStatus)(equalTo(LoyaltyReportStatus.APPLIED)) &&
            assert(clientsChangedBufferResult)(
              hasSameElements(
                List(
                  InputRecord(clientId, "loyalty_report"),
                  InputRecord(clientId, "dealer_pony")
                )
              )
            )
        },
        testM("set approve on report with status = pre_approved") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReport = createBaseInitialReport(periodId, clientId)

            _ <- insertLoyaltyReport(firstReport, LoyaltyReportStatus.PRE_APPROVED)(xa)

            toApprove = NonEmptyList.of(createClientCashbackInfo(clientId))
            notApprovedReports <- loyaltyReportClient.setApprove(approvingUser, periodId, toApprove)

            updatedReport <- loyaltyReportClient.findByPeriodID(periodId)
            updatedReportChecks = assert(updatedReport)(hasSize(equalTo(1))) &&
              assert(updatedReport.head.getClientId)(equalTo(clientId)) &&
              assert(updatedReport.head.getStatus)(equalTo(LoyaltyReportStatus.APPROVED)) &&
              assert(updatedReport.head.getApprovals.getApprovedBy)(equalTo(approvingUser)) &&
              assert(updatedReport.head.getApprovals.getApprovedDate.getSeconds)(isGreaterThan(0L))
          } yield assert(notApprovedReports)(isEmpty) && updatedReportChecks
        },
        testM("find not approved reports with active period") {
          val fourthClientId = 18L
          val fifthClientId = 19L

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1))
            _ <- cashbackClient.insert(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))
            periods <- cashbackClient.getPeriods.map(_.sortBy(_.finish).reverse)
            activePeriodId = periods.head.id
            closedPeriodId = periods(1).id
            _ <- cashbackClient.closeById(closedPeriodId)

            activeFirstReport = createBaseInitialReport(activePeriodId, clientId)
            _ <- insertLoyaltyReport(activeFirstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            activeFirstId <- selectExactLoyaltyReportId(activePeriodId, clientId)(xa)

            closedFirstReport = createBaseInitialReport(closedPeriodId, clientId)
            _ <- insertLoyaltyReport(closedFirstReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            firstActiveNotApproved <- loyaltyReportClient.findActiveNotApproved(clientId)

            activeSecondReport = createBaseInitialReport(activePeriodId, secondClientId)
            _ <- insertLoyaltyReport(activeSecondReport, LoyaltyReportStatus.IN_PROGRESS_NEGATIVE)(xa)
            activeSecondId <- selectExactLoyaltyReportId(activePeriodId, secondClientId)(xa)
            secondActiveNotApproved <- loyaltyReportClient.findActiveNotApproved(secondClientId)

            activeThirdReport = createBaseInitialReport(activePeriodId, thirdClientId)
            _ <- insertLoyaltyReport(activeThirdReport, LoyaltyReportStatus.PRE_APPROVED)(xa)
            activeThirdId <- selectExactLoyaltyReportId(activePeriodId, thirdClientId)(xa)
            thirdActiveNotApproved <- loyaltyReportClient.findActiveNotApproved(thirdClientId)

            activeFourthReport = createBaseInitialReport(activePeriodId, fourthClientId)
            _ <- insertLoyaltyReport(activeFourthReport, LoyaltyReportStatus.APPROVED)(xa)
            fourthActiveNotApproved <- loyaltyReportClient.findActiveNotApproved(fourthClientId)

            activeFifthReport = createBaseInitialReport(activePeriodId, fifthClientId)
            _ <- insertLoyaltyReport(activeFifthReport, LoyaltyReportStatus.APPROVED)(xa)
            fifthActiveNotApproved <- loyaltyReportClient.findActiveNotApproved(fifthClientId)
          } yield assert(firstActiveNotApproved.map(_.id))(hasSameElements(List(activeFirstId))) &&
            assert(secondActiveNotApproved.map(_.id))(hasSameElements(List(activeSecondId))) &&
            assert(thirdActiveNotApproved.map(_.id))(hasSameElements(List(activeThirdId))) &&
            assert(fourthActiveNotApproved)(isEmpty) &&
            assert(fifthActiveNotApproved)(isEmpty)
        },
        testM("actualize loyalty report params and cashback for IN_PROGRESS") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstClientReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(firstClientReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            secondClientReport = createBaseInitialReport(periodId, secondClientId)
            _ <- insertLoyaltyReport(secondClientReport, LoyaltyReportStatus.IN_PROGRESS_NEGATIVE)(xa)

            reportIds <- selectLoyaltyReportIds(xa)
            firstItem = createBaseReportItem(reportIds.head)
            secondItem = createBaseReportItem(reportIds.last)
            _ <- insertLoyaltyReportItem(firstItem)(xa)
            _ <- insertLoyaltyReportItem(secondItem)(xa)

            reportActualization = NonEmptyList.of(
              LoyaltyReportActualization(
                id = reportIds.head,
                extrabonus = Some(ExtraBonus.OVER_2000_CARS),
                hasFullStock = true,
                cashbackPercent = 3,
                cashbackAmount = 150,
                placementDiscount = Some(15),
                updatedItems = List(createBaseReportItem(reportIds.head, value = 2, resolution = true))
              ),
              LoyaltyReportActualization(
                id = reportIds.last,
                extrabonus = None,
                hasFullStock = true,
                cashbackPercent = 4,
                cashbackAmount = 200,
                placementDiscount = None,
                updatedItems = List(createBaseReportItem(reportIds.last, value = 3, resolution = true))
              )
            )
            _ <- loyaltyReportClient.actualizeReports(reportActualization)

            updatedReports <- loyaltyReportClient.findByPeriodID(periodId).map(_.groupBy(_.getClientId))

            firstClientReportChecks = assert(updatedReports(clientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(clientId).head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(updatedReports(clientId).head.getExtraBonus)(equalTo(ExtraBonus.OVER_2000_CARS)) &&
              assert(updatedReports(clientId).head.getHasFullStock)(isTrue) &&
              assert(updatedReports(clientId).head.getCashbackPercent)(equalTo(3)) &&
              assert(updatedReports(clientId).head.getCashbackAmount)(equalTo(150L)) &&
              assert(updatedReports(clientId).head.getPlacementDiscountPercent)(equalTo(15))

            secondClientReportChecks = assert(updatedReports(secondClientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(secondClientId).head.getStatus)(
                equalTo(LoyaltyReportStatus.IN_PROGRESS_NEGATIVE)
              ) &&
              assert(updatedReports(secondClientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(secondClientId).head.getHasFullStock)(isTrue) &&
              assert(updatedReports(secondClientId).head.getCashbackPercent)(equalTo(4)) &&
              assert(updatedReports(secondClientId).head.getCashbackAmount)(equalTo(200L)) &&
              assert(updatedReports(secondClientId).head.getPlacementDiscountPercent)(equalTo(0))

            items <-
              sql"""
                SELECT `value`, `resolution`
                FROM loyalty_report_item
                WHERE report_id IN (${reportIds.head}, ${reportIds.last})
                AND criterion=${LoyaltyCriteria.ExtraBonus.toString}
                """
                .query[(Int, Boolean)]
                .to[List]
                .transact(xa)
          } yield firstClientReportChecks &&
            secondClientReportChecks &&
            assert(items)(hasSameElements(List((2, true), (3, true))))
        },
        testM("actualize loyalty report params and cashback for PRE_APPROVED") {
          val fourthClientId = 26L

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- insertLoyaltyReport(createBaseInitialReport(periodId, clientId), LoyaltyReportStatus.PRE_APPROVED)(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, secondClientId),
              LoyaltyReportStatus.PRE_APPROVED
            )(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, thirdClientId).copy(cashbackAmount = 200, cashbackPercent = 4),
              LoyaltyReportStatus.PRE_APPROVED
            )(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, fourthClientId),
              LoyaltyReportStatus.PRE_APPROVED
            )(xa)

            reportIds <- selectLoyaltyReportIds(xa)
            reportActualization = NonEmptyList.fromListUnsafe(
              List(
                LoyaltyReportActualization(
                  id = reportIds.head,
                  extrabonus = Some(ExtraBonus.OVER_2000_CARS),
                  hasFullStock = true,
                  cashbackPercent = 3,
                  cashbackAmount = 150,
                  placementDiscount = Some(10),
                  updatedItems = Nil
                ),
                LoyaltyReportActualization(
                  id = reportIds(1),
                  extrabonus = None,
                  hasFullStock = true,
                  cashbackPercent = 2,
                  cashbackAmount = 100,
                  placementDiscount = Some(0),
                  updatedItems = Nil
                ),
                LoyaltyReportActualization(
                  id = reportIds(2),
                  extrabonus = None,
                  hasFullStock = false,
                  cashbackPercent = 4,
                  cashbackAmount = 200,
                  placementDiscount = Some(0),
                  updatedItems = Nil
                ),
                LoyaltyReportActualization(
                  id = reportIds(3),
                  extrabonus = None,
                  hasFullStock = false,
                  cashbackPercent = 2,
                  cashbackAmount = 100,
                  placementDiscount = Some(25),
                  updatedItems = Nil
                )
              )
            )
            _ <- loyaltyReportClient.actualizeReports(reportActualization)

            updatedReports <- loyaltyReportClient.findByPeriodID(periodId).map(_.groupBy(_.getClientId))

            firstClientReportChecks = assert(updatedReports(clientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(clientId).head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(updatedReports(clientId).head.getExtraBonus)(equalTo(ExtraBonus.OVER_2000_CARS)) &&
              assert(updatedReports(clientId).head.getHasFullStock)(isTrue) &&
              assert(updatedReports(clientId).head.getCashbackPercent)(equalTo(3)) &&
              assert(updatedReports(clientId).head.getCashbackAmount)(equalTo(150L)) &&
              assert(updatedReports(clientId).head.getPlacementDiscountPercent)(equalTo(10))

            secondClientReportChecks = assert(updatedReports(secondClientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(secondClientId).head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(updatedReports(secondClientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(secondClientId).head.getHasFullStock)(isTrue) &&
              assert(updatedReports(secondClientId).head.getCashbackPercent)(equalTo(2)) &&
              assert(updatedReports(secondClientId).head.getCashbackAmount)(equalTo(100L)) &&
              assert(updatedReports(secondClientId).head.getPlacementDiscountPercent)(equalTo(0))

            thirdClientReportChecks = assert(updatedReports(thirdClientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(thirdClientId).head.getStatus)(equalTo(LoyaltyReportStatus.PRE_APPROVED)) &&
              assert(updatedReports(thirdClientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(thirdClientId).head.getHasFullStock)(isFalse) &&
              assert(updatedReports(thirdClientId).head.getCashbackPercent)(equalTo(4)) &&
              assert(updatedReports(thirdClientId).head.getCashbackAmount)(equalTo(200L)) &&
              assert(updatedReports(thirdClientId).head.getPlacementDiscountPercent)(equalTo(0))

            fourthClientReportChecks = assert(updatedReports(fourthClientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(fourthClientId).head.getStatus)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
              assert(updatedReports(fourthClientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(fourthClientId).head.getHasFullStock)(isFalse) &&
              assert(updatedReports(fourthClientId).head.getCashbackPercent)(equalTo(2)) &&
              assert(updatedReports(fourthClientId).head.getCashbackAmount)(equalTo(100L)) &&
              assert(updatedReports(fourthClientId).head.getPlacementDiscountPercent)(equalTo(25))
          } yield firstClientReportChecks && secondClientReportChecks && thirdClientReportChecks && fourthClientReportChecks
        },
        testM("not actualize loyalty report params and cashback for APPROVED/APPLIED") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstClientReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(firstClientReport, LoyaltyReportStatus.APPROVED)(xa)
            secondClientReport = createBaseInitialReport(periodId, secondClientId)
            _ <- insertLoyaltyReport(secondClientReport, LoyaltyReportStatus.APPLIED)(xa)

            reportIds <- selectLoyaltyReportIds(xa)

            reportActualization = NonEmptyList.of(
              LoyaltyReportActualization(
                id = reportIds.head,
                extrabonus = Some(ExtraBonus.OVER_2000_CARS),
                hasFullStock = true,
                cashbackPercent = 3,
                cashbackAmount = 150,
                placementDiscount = Some(20),
                updatedItems = Nil
              ),
              LoyaltyReportActualization(
                id = reportIds.last,
                extrabonus = None,
                hasFullStock = true,
                cashbackPercent = 4,
                cashbackAmount = 200,
                placementDiscount = Some(40),
                updatedItems = Nil
              )
            )
            _ <- loyaltyReportClient.actualizeReports(reportActualization)

            updatedReports <- loyaltyReportClient.findByPeriodID(periodId).map(_.groupBy(_.getClientId))

            firstClientReportChecks = assert(updatedReports(clientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(clientId).head.getStatus)(equalTo(LoyaltyReportStatus.APPROVED)) &&
              assert(updatedReports(clientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(clientId).head.getHasFullStock)(isFalse) &&
              assert(updatedReports(clientId).head.getCashbackPercent)(equalTo(2)) &&
              assert(updatedReports(clientId).head.getCashbackAmount)(equalTo(100L)) &&
              assert(updatedReports(clientId).head.getPlacementDiscountPercent)(equalTo(0))

            secondClientReportChecks = assert(updatedReports(secondClientId))(hasSize(equalTo(1))) &&
              assert(updatedReports(secondClientId).head.getStatus)(
                equalTo(LoyaltyReportStatus.APPLIED)
              ) &&
              assert(updatedReports(secondClientId).head.getExtraBonus)(equalTo(ExtraBonus.UNKNOWN_BONUS)) &&
              assert(updatedReports(secondClientId).head.getHasFullStock)(isFalse) &&
              assert(updatedReports(secondClientId).head.getCashbackPercent)(equalTo(2)) &&
              assert(updatedReports(secondClientId).head.getCashbackAmount)(equalTo(100L)) &&
              assert(updatedReports(secondClientId).head.getPlacementDiscountPercent)(equalTo(0))
          } yield firstClientReportChecks && secondClientReportChecks
        },
        testM("find active approved reports") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            _ <- cashbackClient.insert(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            periods <- cashbackClient.getPeriods.map(_.sortBy(_.finish).reverse)
            activePeriodId = periods.head.id
            closedPeriodId = periods(1).id
            _ <- cashbackClient.closeById(closedPeriodId)

            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, clientId).copy(
                vasSpendPercent = Some(10),
                placementSpendPercent = Some(90),
                resolution = true
              ),
              LoyaltyReportStatus.APPROVED
            )(xa)
            reportId <- selectExactLoyaltyReportId(activePeriodId, clientId)(xa)

            // active period but not approved status
            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, secondClientId),
              LoyaltyReportStatus.IN_PROGRESS
            )(xa)

            // approved status but not active period
            _ <- insertLoyaltyReport(
              createBaseInitialReport(closedPeriodId, thirdClientId).copy(resolution = true),
              LoyaltyReportStatus.APPROVED
            )(xa)

            result <- loyaltyReportClient.findActiveApproved
          } yield assert(result)(
            equalTo(
              List(
                ActiveApprovedReportInfo(
                  id = reportId,
                  clientId = clientId,
                  periodId = activePeriodId,
                  cashbackAmount = 100,
                  proportions = CashbackProportions.of(10, 90).toOption.flatten,
                  placementDiscountPercent = 0,
                  resolution = true
                )
              )
            )
          )
        },
        testM("find active negative reports") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            _ <- cashbackClient.insert(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            periods <- cashbackClient.getPeriods.map(_.sortBy(_.finish).reverse)
            activePeriodId = periods.head.id
            closedPeriodId = periods(1).id
            _ <- cashbackClient.closeById(closedPeriodId)

            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, clientId),
              LoyaltyReportStatus.APPROVED
            )(xa)
            reportId <- selectExactLoyaltyReportId(activePeriodId, clientId)(xa)

            // active period but positive status
            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, secondClientId),
              LoyaltyReportStatus.IN_PROGRESS
            )(xa)

            // negative status but not active period
            _ <- insertLoyaltyReport(
              createBaseInitialReport(closedPeriodId, thirdClientId),
              LoyaltyReportStatus.APPROVED
            )(xa)

            result <- loyaltyReportClient.findActiveNegative
          } yield assert(result)(equalTo(List(ActiveNegativeReportInfo(reportId, clientId, activePeriodId))))
        },
        testM("not return active negative reports if they've already been pushed") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            _ <- cashbackClient.insert(LocalDate.now().minusDays(20), LocalDate.now().minusDays(11))
            periods <- cashbackClient.getPeriods.map(_.sortBy(_.finish).reverse)
            activePeriodId = periods.head.id

            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, clientId),
              LoyaltyReportStatus.APPROVED
            )(xa)
            reportId <- selectExactLoyaltyReportId(activePeriodId, clientId)(xa)

            _ <- insertLoyaltyReport(
              createBaseInitialReport(activePeriodId, secondClientId),
              LoyaltyReportStatus.APPROVED,
              negativeResolutionPushed = true
            )(xa)

            result <- loyaltyReportClient.findActiveNegative
          } yield assert(result)(equalTo(List(ActiveNegativeReportInfo(reportId, clientId, activePeriodId))))
        },
        testM("mark reports applied") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- insertLoyaltyReport(createBaseInitialReport(periodId, clientId), LoyaltyReportStatus.APPROVED)(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, secondClientId),
              LoyaltyReportStatus.APPROVED
            )(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, thirdClientId),
              LoyaltyReportStatus.IN_PROGRESS
            )(xa)

            reportIds <- selectLoyaltyReportIds(xa)
            _ <- loyaltyReportClient.markApplied(NonEmptyList.fromListUnsafe(reportIds))

            reports <- loyaltyReportClient.findByPeriodID(periodId)
            result = reports.map(report => (report.getClientId, report.getStatus))
          } yield assert(result)(
            hasSameElements(
              List(
                (clientId, LoyaltyReportStatus.APPLIED),
                (secondClientId, LoyaltyReportStatus.APPLIED),
                (thirdClientId, LoyaltyReportStatus.IN_PROGRESS)
              )
            )
          )
        },
        testM("find current report and items") {
          val notExistedClientId = 100L

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1))
            periodId <- cashbackClient.getPeriods.map(_.head.id)

            initialReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(initialReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            reportIds <- selectLoyaltyReportIds(xa)

            firstItem = createBaseReportItem(reportIds.head)
            secondItem = createBaseReportItem(reportIds.head, criterion = LoyaltyCriteria.SiteCheck)
            _ <- insertLoyaltyReportItem(firstItem)(xa)
            _ <- insertLoyaltyReportItem(secondItem)(xa)

            resultWithItems <- loyaltyReportClient
              .findCurrentWithItems(clientId)
              .map { reports =>
                reports.map { case (report, items) => (InitialLoyaltyReport.fromLoyaltyReport(report), items.toSet) }
              }
            resultWithoutItems <- loyaltyReportClient.findCurrent(clientId)
            emptyResult <- loyaltyReportClient.findCurrentWithItems(notExistedClientId)
          } yield assert(resultWithItems)(isSome(equalTo(initialReport -> Set(firstItem, secondItem)))) &&
            assert(resultWithoutItems.map(InitialLoyaltyReport.fromLoyaltyReport))(isSome(equalTo(initialReport))) &&
            assert(emptyResult)(isNone)
        },
        testM("find previous report") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            cashbackClient = new JdbcCashbackPeriodDao(xa)
            _ <- cashbackClient.insert(LocalDate.now().minusDays(2), LocalDate.now().minusDays(1))
            _ <- cashbackClient.insert(LocalDate.now().minusDays(4), LocalDate.now().minusDays(3))
            periods <- cashbackClient.getPeriods.map(_.sortBy(_.finish).reverse)
            currentPeriodId = periods.head.id
            previousPeriodId = periods(1).id

            previousPeriodReport = createBaseInitialReport(previousPeriodId, clientId)
            _ <- insertLoyaltyReport(previousPeriodReport, LoyaltyReportStatus.IN_PROGRESS)(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(currentPeriodId, clientId),
              LoyaltyReportStatus.IN_PROGRESS
            )(xa)

            result <- loyaltyReportClient.findPrevious(clientId)
          } yield assert(result.map(InitialLoyaltyReport.fromLoyaltyReport))(isSome(equalTo(previousPeriodReport)))
        },
        testM("mark negative reports pushed") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, clientId),
              LoyaltyReportStatus.APPROVED
            )(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, secondClientId),
              LoyaltyReportStatus.IN_PROGRESS
            )(xa)
            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, thirdClientId),
              LoyaltyReportStatus.IN_PROGRESS_NEGATIVE
            )(xa)
            reportIds <- selectLoyaltyReportIds(xa)

            _ <- loyaltyReportClient.markNegativePushed(NonEmptyList.fromListUnsafe(reportIds))
            result <-
              sql"""
                SELECT `client_id`, `negative_resolution_pushed`
                FROM loyalty_report
                WHERE period_id=$periodId
                """
                .query[(Long, Boolean)]
                .to[List]
                .transact(xa)
          } yield assert(result)(
            hasSameElements(List((clientId, true), (secondClientId, false), (thirdClientId, false)))
          )
        },
        testM("not reset NEGATIVE_RESOLUTION_PUSHED when positive resolution comes") {
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

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- insertLoyaltyReport(
              createBaseInitialReport(periodId, clientId),
              status = LoyaltyReportStatus.APPROVED,
              negativeResolutionPushed = true
            )(xa)
            initialResult <-
              sql"""
                SELECT `status`, `negative_resolution_pushed`
                FROM loyalty_report
                WHERE client_id=$clientId AND period_id=$periodId
                """
                .query[(LoyaltyReportStatus, Boolean)]
                .to[List]
                .transact(xa)

            _ <- loyaltyReportClient.upsert(updateReportMsg, items = Nil)
            updatedResult <-
              sql"""
                SELECT `negative_resolution_pushed`
                FROM loyalty_report
                WHERE client_id=$clientId AND period_id=$periodId
                """
                .query[Boolean]
                .to[List]
                .transact(xa)
          } yield assert(initialResult)(equalTo(List((LoyaltyReportStatus.APPROVED, true)))) &&
            assert(updatedResult)(equalTo(List(true)))
        },
        testM("find activations for report") {
          val activationsAmount = 123L

          val report = LoyaltyReport
            .newBuilder()
            .setClientId(clientId)
            .setPeriodId(periodId)
            .setResolution(false)
            .setLoyaltyLevel(0)
            .setCashbackAmount(0)
            .setCashbackPercent(0)
            .setActivationsAmount(activationsAmount)
            .setHasFullStock(false)
            .build()

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- loyaltyReportClient.upsert(report, Nil)
            foundedActivationsAmount <- loyaltyReportClient.findActivationsAmount(clientId, periodId)
          } yield assert(foundedActivationsAmount)(isSome(equalTo(activationsAmount.toDouble)))
        },
        testM("return none if no activations amount") {
          val dummyClientId = 666L
          val dummyPeriodId = 10000L

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)
            result <- loyaltyReportClient.findActivationsAmount(dummyClientId, dummyPeriodId)
          } yield assert(result)(isNone)
        },
        testM("get reports by periodID") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            firstReportForFirstPeriod =
              createBaseInitialReport(periodId, clientId).copy(hasFullStock = true)
            secondReportForFirstPeriod =
              createBaseInitialReport(periodId, secondClientId).copy(extraBonus = Some(ExtraBonus.OVER_2000_CARS))
            reportForSecondPeriod =
              createBaseInitialReport(secondPeriodId, clientId).copy(extraBonus = Some(ExtraBonus.OVER_2000_CARS))

            _ <- insertLoyaltyReport(firstReportForFirstPeriod, LoyaltyReportStatus.APPLIED)(xa)
            _ <- insertLoyaltyReport(secondReportForFirstPeriod, LoyaltyReportStatus.APPLIED)(xa)
            _ <- insertLoyaltyReport(reportForSecondPeriod, LoyaltyReportStatus.APPLIED)(xa)

            reports <- loyaltyReportClient.findByPeriodID(periodId)
          } yield assert(reports)(hasSize(equalTo(2))) &&
            assert(reports.map(InitialLoyaltyReport.fromLoyaltyReport))(
              hasSameElements(List(firstReportForFirstPeriod, secondReportForFirstPeriod))
            )
        },
        testM("revoke marks report as IN_PROGRESS, set user and timestamp") {
          def selectApprovalsFields(clientId: ClientId, periodId: PeriodId)(xa: Transactor[Task]) =
            sql"""
                SELECT `pre_approved_by`, `pre_approved_date`, `approved_by`, `approved_date`
                FROM loyalty_report
                WHERE client_id=$clientId AND period_id=$periodId
                """
              .query[(Option[String], Option[Instant], Option[String], Option[Instant])]
              .to[List]
              .transact(xa)

          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            initialReport = createBaseInitialReport(periodId, clientId)
            _ <- insertLoyaltyReport(initialReport, LoyaltyReportStatus.APPLIED)(xa)

            approvalsTime = Instant.now().truncatedTo(ChronoUnit.MILLIS)
            _ <-
              sql"""
                UPDATE loyalty_report
                SET `pre_approved_by` = 'user1',
                    `pre_approved_date` = $approvalsTime,
                    `approved_by` = 'user2',
                    `approved_date` = $approvalsTime
                WHERE client_id=$clientId AND period_id=$periodId
                """.update.run
                .transact(xa)

            approvalsBeforeRevoke <- selectApprovalsFields(clientId, periodId)(xa)

            userId = "Unit Tester"
            _ <- loyaltyReportClient.revoke(clientId, periodId, userId)

            updatedInfo <-
              sql"""
                SELECT `status`, `revoked_by`, `revoked_date`
                FROM loyalty_report
                WHERE client_id=$clientId AND period_id=$periodId
                """
                .query[(LoyaltyReportStatus, String, Instant)]
                .to[List]
                .transact(xa)

            (status, revokedBy, revokedDate) = updatedInfo.head

            approvalsAfterRevoke <- selectApprovalsFields(clientId, periodId)(xa)
          } yield assert(updatedInfo)(hasSize(equalTo(1))) &&
            assert(status)(equalTo(LoyaltyReportStatus.IN_PROGRESS)) &&
            assert(revokedBy)(equalTo(userId)) &&
            assert(revokedDate.toEpochMilli)(isGreaterThan(0L)) &&
            assert(approvalsBeforeRevoke)(hasSize(equalTo(1))) &&
            assert(approvalsBeforeRevoke.head)(
              equalTo((Some("user1"), Some(approvalsTime), Some("user2"), Some(approvalsTime)))
            ) &&
            assert(approvalsAfterRevoke)(hasSize(equalTo(1))) &&
            assert(approvalsAfterRevoke.head)(equalTo((None, None, None, None)))
        }
      ),
      suite("Loyalty report comment operation")(
        testM("successfully insert new comment for existing period") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.addComment(1L, 1L, "test comment", "Unit Tester")
          } yield assert(result)(equalTo(Updated))
        },
        testM("fail if comment exists") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- loyaltyReportClient.addComment(1L, 1L, "test comment", "Unit Tester")
            result <- loyaltyReportClient.addComment(1L, 1L, "test comment2", "Unit Tester")
          } yield assert(result)(equalTo(CommentAlreadyExisted))
        },
        testM("fail if period not found") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.addComment(123L, 1L, "test comment", "Unit Tester")
          } yield assert(result)(equalTo(LoyaltyReportNotFound))
        },
        testM("fail if client not found") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.addComment(1L, 123L, "test comment", "Unit Tester")
          } yield assert(result)(equalTo(LoyaltyReportNotFound))
        },
        testM("successfully delete existing comment") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            _ <- loyaltyReportClient.addComment(1L, 1L, "test comment", "Unit Tester")
            result <- loyaltyReportClient.deleteComment(1L, 1L, "Unit Tester")
          } yield assert(result)(isTrue)
        },
        testM("do not fail on delete if comment doesn't exist") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.deleteComment(1L, 1L, "Unit Tester")
          } yield assert(result)(isTrue)
        },
        testM("fail to delete if period is not found") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.deleteComment(2L, 1L, "Unit Tester")
          } yield assert(result)(isFalse)
        },
        testM("fail to delete if client is not found") {
          for {
            xa <- ZIO.service[Transactor[Task]]
            loyaltyReportClient = new JdbcLoyaltyReportDao(xa)

            result <- loyaltyReportClient.deleteComment(1L, 2L, "Unit Tester")
          } yield assert(result)(isFalse)
        }
      ) @@ before(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            val report = createBaseInitialReport(periodId = 1L, clientId = 1L).copy(hasFullStock = true)
            for {
              _ <- insertLoyaltyReport(report, LoyaltyReportStatus.APPLIED)(xa)
            } yield ()
          }
      )
    ) @@
      beforeAll(
        ZIO
          .service[Transactor[Task]]
          .flatMap { xa =>
            for {
              _ <- InitSchema("/schema.sql", xa)
            } yield ()
          }
      ) @@
      after(
        ZIO.service[Transactor[Task]].flatMap { xa =>
          sql"DELETE FROM loyalty_report_item".update.run.transact(xa) *>
            sql"DELETE FROM loyalty_report".update.run.transact(xa) *>
            sql"DELETE FROM cashback_periods".update.run.transact(xa) *>
            sql"DELETE FROM clients_changed_buffer".update.run.transact(xa)
        }
      ) @@ sequential).provideCustomLayerShared(TestMySQL.managedTransactor)
  }
}
