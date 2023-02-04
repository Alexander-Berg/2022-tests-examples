package ru.auto.salesman.dao.jdbc

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import ru.auto.cabinet.ApiModel.ExtraBonus
import ru.auto.salesman.dao.impl.jdbc._
import ru.auto.salesman.dao.slick.invariant.GetResult
import ru.auto.salesman.dao.slick.invariant.StaticQuery.interpolation
import ru.auto.salesman.dao.testkit.DatabaseFunctionsDao
import ru.auto.salesman.dao.{
  CashbackPeriodDao,
  ClientsChangedBufferDao,
  LoyaltyReportDaoSpec
}
import ru.auto.salesman.model.cashback._
import ru.auto.salesman.model.{ClientId, Funds, PeriodId}
import ru.auto.salesman.test.template.SalesmanLoyaltyReportJdbcSpecTemplate

import scala.slick.jdbc.SetParameter

class JdbcLoyaltyReportDaoSpec
    extends LoyaltyReportDaoSpec
    with SalesmanLoyaltyReportJdbcSpecTemplate
    with BeforeAndAfterAll {

  override def afterAll: Unit =
    database.withSession { implicit session =>
      session.conn.prepareStatement("DELETE FROM loyalty_report").execute
      session.conn.prepareStatement("DELETE FROM cashback_periods").execute
      session.conn
        .prepareStatement("DELETE FROM clients_changed_buffer")
        .execute
    }

  def cashbackPeriodDao: CashbackPeriodDao =
    new JdbcCashbackPeriodDao(database)

  def loyaltyReportDao: TestLoyaltyReportDaoExtensions =
    new JdbcLoyaltyReportDao(database) with TestLoyaltyReportDaoExtensions {

      implicit private val testReportResult: GetResult[LoyaltyReportTest] =
        GetResult[LoyaltyReportTest] { r =>
          val id = r.<<[Long]
          val periodId = PeriodId(r.<<[PeriodId.Raw])
          val clientId = r.<<[ClientId]
          val status = r.<<[String]
          val preApprovedBy = r.<<?[String]
          val preApprovedDate =
            r.<<?[DateTime](ru.auto.salesman.dao.impl.jdbc.DateTimeOptionResult)
          val approvedBy = r.<<?[String]
          val approvedDate =
            r.<<?[DateTime](ru.auto.salesman.dao.impl.jdbc.DateTimeOptionResult)
          val extraBonus = r.<<?[ExtraBonus]
          val hasFullStock = Some(r.<<[Boolean])
          val cashbackPercent = Some(r.<<[Int])
          val cashbackAmount = Some(r.<<[Funds])
          val negativeResolutionPushed = r.<<[Boolean]
          val placementDiscountPercent = r.<<?[Int]
          val revokedBy = r.<<?[String]
          val revokedDate =
            r.<<?[DateTime](ru.auto.salesman.dao.impl.jdbc.DateTimeOptionResult)
          val placementDiscountOverride = PlacementDiscountOverride.fromOptions(
            r.<<?[Int],
            r.<<?[String],
            r.<<?[String],
            r.<<?[DateTime](ru.auto.salesman.dao.impl.jdbc.DateTimeOptionResult)
          )

          LoyaltyReportTest(
            id,
            periodId,
            clientId,
            status,
            preApprovedBy,
            preApprovedDate,
            approvedBy,
            approvedDate,
            extraBonus,
            hasFullStock,
            cashbackPercent,
            cashbackAmount,
            negativeResolutionPushed,
            placementDiscountPercent,
            revokedBy,
            revokedDate,
            placementDiscountOverride
          )
        }

      def find(periodId: PeriodId, clientId: ClientId): LoyaltyReportTest =
        database.withSession { implicit session =>
          sql"""
             select
               id,
               period_id,
               client_id,
               status,
               pre_approved_by,
               pre_approved_date,
               approved_by,
               approved_date,
               extra_bonus,
               has_full_stock,
               cashback_percent,
               cashback_amount,
               negative_resolution_pushed,
               placement_discount_percent,
               revoked_by,
               revoked_date,
               placement_discount_percent_override,
               placement_discount_editor,
               placement_discount_edit_comment,
               placement_discount_edit_date
             from loyalty_report
             where period_id = $periodId and client_id = $clientId
             """.as[LoyaltyReportTest].first

        }

      def plainInsert(
          initial: InitialLoyaltyReport,
          status: ApiModel.LoyaltyReportStatus,
          negativeResolutionPushed: Boolean = false,
          managerName: String = "",
          approvedAt: Option[DateTime] = None
      ): Long =
        database.withTransaction { implicit session =>
          import initial._
          sqlu"""
            INSERT INTO loyalty_report (`period_id`, `client_id`, `loyalty_level`, `cashback_amount`, `cashback_percent`, `extra_bonus`, `has_full_stock`, `resolution`, `activations_amount`, `status`, `negative_resolution_pushed`, `manager_name`, `vas_spend_percent`, `placement_spend_percent`, `placement_discount_percent`, `approved_date`)
            VALUES ($periodId, $clientId, $loyaltyLevel, $cashbackAmount, $cashbackPercent, $extraBonus, $hasFullStock, $resolution, 0, $status, $negativeResolutionPushed, $managerName, $vasSpendPercent, $placementSpendPercent, $placementDiscountPercent, $approvedAt);
          """.execute
          sql"""
            SELECT id FROM loyalty_report WHERE period_id=$periodId AND client_id=$clientId;
          """.as[Long].list.head
        }

      implicit private val loyaltyStatusParameter: SetParameter[ApiModel.LoyaltyReportStatus] =
        SetParameter[ApiModel.LoyaltyReportStatus]((ll, pp) =>
          pp.setString(ll.toString.toLowerCase())
        )

      implicit private val extraBonusParameter: SetParameter[Option[ExtraBonus]] =
        SetParameter[Option[ExtraBonus]]((b, pp) => pp.setStringOption(b.map(_.toString)))

      implicit private val extraBonusResult: GetResult[Option[ExtraBonus]] =
        GetResult(r => r.<<?[String].map(ExtraBonus.valueOf))

      implicit private val loyaltyLevelParameter: SetParameter[LoyaltyLevel] =
        SetParameter[LoyaltyLevel]((ll, pp) => pp.setInt(ll.raw))

      def insertItem(item: LoyaltyReportItem): Unit =
        database.withSession { implicit session =>
          import item.data._
          sqlu"""
            INSERT INTO loyalty_report_item(`report_id`, `criterion`, `value`, `resolution`, `comment`, `epoch`)
            VALUES(${item.reportId}, $criterion, ${item.data.value}, $resolution, $comment, $epoch)
          """.execute
        }

      def findItem(
          reportId: Long,
          criterion: LoyaltyCriteria
      ): LoyaltyReportItem =
        database.withSession { implicit session =>
          sql"""
            SELECT
              `report_id`, `criterion`, `value`, `resolution`, `comment`, `epoch`, `required`
            FROM loyalty_report_item
            WHERE report_id=$reportId AND criterion=${criterion.toString}
          """.as[LoyaltyReportItem].list.head
        }

      implicit private def loyaltyReportItemResult: GetResult[LoyaltyReportItem] =
        GetResult[LoyaltyReportItem] { r =>
          val reportId = r.<<[Long]
          val criterion = r.<<[String]
          val value = r.<<[Long]
          val resolution = r.<<[Boolean]
          val comment = r.<<?[String]
          val epoch = r.<<[DateTime]
          LoyaltyReportItem(
            reportId = reportId,
            data = LoyaltyReportItemData(
              criterion = criterion,
              value = value,
              resolution = resolution,
              comment = comment,
              epoch = epoch
            )
          )
        }

      def findItems(reportId: ClientId): List[LoyaltyReportItem] =
        database.withSession { implicit session =>
          sql"""
            SELECT
              `report_id`, `criterion`, `value`, `resolution`, `comment`, `epoch`, `required`
            FROM loyalty_report_item
            WHERE report_id=$reportId
          """.as[LoyaltyReportItem].list
        }
    }

  def clientsChangedBufferDao: ClientsChangedBufferDao =
    new JdbcClientsChangedBufferDao(database)

  def databaseFunctionsDao: DatabaseFunctionsDao =
    new DatabaseFunctionsDao(database)
}
