package ru.yandex.realty.cadastr.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.cadastr.dao.actions.impl.AddressInfoDbActionsImpl.AddressInfoTable
import ru.yandex.realty.cadastr.dao.actions.impl.ExcerptDbActionsImpl.ExcerptTable
import ru.yandex.realty.cadastr.dao.actions.impl.ReportDbActionsImpl.{HistoryTable => ReportHistoryTable}
import ru.yandex.realty.cadastr.dao.actions.impl.OfferDbActionsImpl.OfferTable
import ru.yandex.realty.cadastr.dao.actions.impl.PaidReportDbActionsImpl.PaidReportTable
import ru.yandex.realty.cadastr.dao.actions.impl.PaidReportVoteDbActionsImpl.PaidReportVoteTable
import ru.yandex.realty.cadastr.dao.actions.impl.ReportDbActionsImpl.ReportTable
import ru.yandex.realty.cadastr.dao.actions.impl.RequestDbActionsImpl.RequestTable
import ru.yandex.realty.cadastr.dao.actions.impl.{
  AddressInfoDbActionsImpl,
  ExcerptDbActionsImpl,
  OfferDbActionsImpl,
  PaidReportDbActionsImpl,
  PaidReportVoteDbActionsImpl,
  ReportDbActionsImpl,
  RequestDbActionsImpl
}
import ru.yandex.realty.cadastr.dao.actions.{
  AddressInfoDbActions,
  ExcerptDbActions,
  OfferDbActions,
  PaidReportDbActions,
  PaidReportVoteDbActions,
  ReportDbActions,
  RequestDbActions
}
import ru.yandex.realty.cadastr.dao.impl.{
  AddressInfoDaoImpl,
  ExcerptDaoImpl,
  OfferDaoImpl,
  PaidReportDaoImpl,
  PaidReportVoteDaoImpl,
  ReportDaoImpl,
  RequestDaoImpl
}
import slick.driver.H2Driver.api._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

trait CadastrDaoBase
  extends WordSpecLike
  with ScalaFutures
  with Matchers
  with Checkers
  with BeforeAndAfter
  with PropertyChecks {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  val database = Database.forConfig("h2mem")
  val masterSlaveDb: MasterSlaveJdbcDatabase = MasterSlaveJdbcDatabase(database, database)

  def excerptDbActions: ExcerptDbActions = new ExcerptDbActionsImpl()
  def offerDbActions: OfferDbActions = new OfferDbActionsImpl()
  def requestDbActions: RequestDbActions = new RequestDbActionsImpl()
  def reportDbActions: ReportDbActions = new ReportDbActionsImpl()
  def addressInfoDbActions: AddressInfoDbActions = new AddressInfoDbActionsImpl()
  def paidReportDbActions: PaidReportDbActions = new PaidReportDbActionsImpl()
  def paidReportVoteDbActions: PaidReportVoteDbActions = new PaidReportVoteDbActionsImpl()

  val excerptDao: ExcerptDao = new ExcerptDaoImpl(excerptDbActions, masterSlaveDb)
  val offerDao: OfferDao = new OfferDaoImpl(offerDbActions, masterSlaveDb)
  val requestDao: RequestDao = new RequestDaoImpl(requestDbActions, masterSlaveDb)
  val reportDao: ReportDao = new ReportDaoImpl(reportDbActions, requestDbActions, excerptDbActions, masterSlaveDb)
  val addressInfoDao: AddressInfoDao = new AddressInfoDaoImpl(addressInfoDbActions, masterSlaveDb)
  val paidReportDao: PaidReportDao = new PaidReportDaoImpl(paidReportDbActions, masterSlaveDb)
  val paidReportVoteDao: PaidReportVoteDao = new PaidReportVoteDaoImpl(paidReportVoteDbActions, masterSlaveDb)

  val offerTable = TableQuery[OfferTable]
  val excerptTable = TableQuery[ExcerptTable]
  val requestTable = TableQuery[RequestTable]
  val reportTable = TableQuery[ReportTable]
  val reportHistoryTable = TableQuery[ReportHistoryTable]
  val addressInfoTable = TableQuery[AddressInfoTable]
  val paidReportTable = TableQuery[PaidReportTable]
  val paidReportVoteTable = TableQuery[PaidReportVoteTable]

  val schemas = offerTable.schema ++ excerptTable.schema ++
    requestTable.schema ++ reportTable.schema ++ reportHistoryTable.schema ++ addressInfoTable.schema ++
    paidReportTable.schema ++ paidReportVoteTable.schema

  def truncateData(): Unit = {
    val truncate = DBIO.seq(schemas.truncate)
    ddlRun(truncate)
  }

  def dropTables(): Unit = {
    val drop = DBIO.seq(schemas.drop)
    ddlRun(drop)
  }

  def createTables(): Unit = {
    val setup = DBIO.seq(schemas.create)
    ddlRun(setup)
  }

  def ddlRun(action: DBIOAction[Unit, NoStream, Effect.Schema]): Unit = {
    val actionFuture = database.run(action)
    Await.result(actionFuture, 5.seconds)
  }
}
