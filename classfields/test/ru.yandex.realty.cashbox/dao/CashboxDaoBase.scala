package ru.yandex.realty.cashbox.dao

import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase
import ru.yandex.realty.cashbox.dao.actions.ReceiptDbActions
import ru.yandex.realty.cashbox.dao.actions.impl.ReceiptDbActionsImpl
import ru.yandex.realty.cashbox.dao.actions.impl.ReceiptDbActionsImpl.ReceiptTable
import ru.yandex.realty.cashbox.dao.impl.ReceiptDaoImpl
import slick.jdbc.H2Profile.api._
import scala.concurrent.duration._

import scala.concurrent.{Await, ExecutionContext, ExecutionContextExecutor}

trait CashboxDaoBase
  extends WordSpecLike
  with ScalaFutures
  with Matchers
  with Checkers
  with BeforeAndAfter
  with PropertyChecks {

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  val database = Database.forConfig("h2mem")
  val masterSlaveDb: MasterSlaveJdbcDatabase = MasterSlaveJdbcDatabase(database, database)

  val receiptDbActions: ReceiptDbActions = new ReceiptDbActionsImpl()

  val receiptDao = new ReceiptDaoImpl(receiptDbActions, masterSlaveDb)

  val receiptTable = TableQuery[ReceiptTable]

  val schemas = receiptTable.schema

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
