package ru.auto.cabinet.test

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.{AsyncTestSuite, BeforeAndAfterAll}
import org.slf4j.LoggerFactory
import ru.auto.cabinet.model.CustomerId
import ru.auto.cabinet.service.instr.DatabaseProxy
import ru.auto.cabinet.trace.Context
import slick.jdbc.MySQLProfile.api.{offsetDateTimeColumnType => _}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

/** Provides database for tests runs
  */
object JdbcSpecTemplate {

  def url: String = MySqlContainer.container.getJdbcUrl
  def user: String = MySqlContainer.container.getUsername
  def password: String = MySqlContainer.container.getPassword

  def office7SchemaScriptPaths = Seq(
    "/sql/office7.sql",
    "/sql/office.final.sql",
    "/sql/cabinet.final.sql",
    "/sql/salesman.sql",
    "/sql/catalog7_yandex.sql",
    "/sql/poi7.sql",
    "/sql/clients_changed_buffer.sql",
    "/sql/moderation.sql",
    "/sql/client_comments.sql",
    "/sql/premoderation_buffer.sql"
  )

  def balanceSchemaScriptPaths = Seq(
    "/sql/balance.sql"
  )

  def poiSchemaScriptPaths = Seq(
    "/sql/poi7.sql"
  )

  def crmSchemaScriptPaths = Seq(
    "/sql/crm.sql"
  )

  def multipostingStatisticsSchemaScriptPaths = Seq(
    "/sql/multiposting_statistics.sql"
  )

  def createHandle =
    TestDatabaseHandle("autoru_cabinet_unit_test", url, user, password)

}

trait JdbcSpecTemplate
    extends Matchers
    with BeforeAndAfterAll
    with ScalaFutures {
  this: AsyncTestSuite =>

  implicit val request = Context.unknown

  val log = LoggerFactory.getLogger(this.getClass)
  val office7Handle: TestDatabaseHandle = JdbcSpecTemplate.createHandle
  val balanceHandle: TestDatabaseHandle = JdbcSpecTemplate.createHandle
  val crmHandle: TestDatabaseHandle = JdbcSpecTemplate.createHandle
  val poiHandle: TestDatabaseHandle = JdbcSpecTemplate.createHandle

  val multipostingStatisticsHandle: TestDatabaseHandle =
    JdbcSpecTemplate.createHandle

  def office7Database: DatabaseProxy = office7Handle.db
  def balanceDatabase: DatabaseProxy = balanceHandle.db
  def crmDatabase: DatabaseProxy = crmHandle.db
  def poiDatabase: DatabaseProxy = poiHandle.db

  def multipostingStatisticsDatabase: DatabaseProxy =
    multipostingStatisticsHandle.db

  override protected def beforeAll(): Unit = {
    val initPoi = poiHandle.init(JdbcSpecTemplate.poiSchemaScriptPaths)
    val initOffice7 =
      office7Handle.init(JdbcSpecTemplate.office7SchemaScriptPaths)
    val initBalance =
      balanceHandle.init(JdbcSpecTemplate.balanceSchemaScriptPaths)
    val initCrm =
      crmHandle.init(JdbcSpecTemplate.crmSchemaScriptPaths)
    val initMultipostingStatistics =
      multipostingStatisticsHandle.init(
        JdbcSpecTemplate.multipostingStatisticsSchemaScriptPaths)
    Await.result(initOffice7, 60 seconds)
    Await.result(initBalance, 60 seconds)
    Await.result(initCrm, 60 seconds)
    Await.result(initMultipostingStatistics, 60 seconds)
    Await.result(initPoi, 60 seconds)
  }

  override protected def afterAll(): Unit = {
    Await.result(office7Handle.drop, 15 seconds)
    Await.result(balanceHandle.drop, 15 seconds)
    Await.result(crmHandle.drop, 15 seconds)
    Await.result(multipostingStatisticsHandle.drop, 15 seconds)
    Await.result(poiHandle.drop, 15 seconds)
  }

  def client1Id = 1

  def wrongClientId = 9

  val agent1 = CustomerId(client1Id, 100)

  def agreementOfferId = 19

}
