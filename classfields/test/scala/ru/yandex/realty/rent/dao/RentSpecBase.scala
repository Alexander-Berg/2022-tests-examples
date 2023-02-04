package ru.yandex.realty.rent.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.scalacheck.{Checkers, ScalaCheckPropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, Suite}
import ru.yandex.realty.application.ng.db.{MasterSlaveJdbcDatabase, MasterSlaveJdbcDatabase2}
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.logging.TracedLogging
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.rent.dao.actions.impl.{
  FlatDbActionsImpl,
  FlatKeyCodeDbActionsImpl,
  FlatQuestionnaireDbActionsImpl,
  FlatShowingDbActionsImpl,
  HouseServiceAuditDbActionsImpl,
  HouseServiceDbActionsImpl,
  InvalidPaymentNotificationDbActionsImpl,
  InventoryDbActionsImpl,
  KeysHandoverDbActionsImpl,
  MeterReadingsDbActionsImpl,
  OwnerRequestDbActionsImpl,
  PaymentDbActionsImpl,
  PaymentHistoryDbActionsImpl,
  PeriodDbActionsImpl,
  RentContractDbActionsImpl,
  RoommateCandidateDbActionsImpl,
  StatusAuditLogDbActionsImpl,
  UserDbActionsImpl,
  UserFlatDbActionsImpl,
  UserShowingDbActionsImpl
}
import ru.yandex.realty.rent.dao.actions.{
  FlatKeyCodeDbActions,
  HouseServiceAuditDbActions,
  HouseServiceDbActions,
  InvalidPaymentNotificationDbActions,
  MeterReadingsDbActions,
  OwnerRequestDbActions,
  PeriodDbActions,
  UserFlatDbActions
}
import ru.yandex.realty.rent.dao.impl.{
  FlatDaoImpl,
  FlatKeyCodeDaoImpl,
  FlatShowingDaoImpl,
  HouseServiceDaoImpl,
  InvalidPaymentNotificationsDaoImpl,
  InventoryDaoImpl,
  MeterReadingsDaoImpl,
  OwnerRequestDaoImpl,
  PaymentDaoImpl,
  PaymentHistoryDaoImpl,
  PeriodDaoImpl,
  RentContractDaoImpl,
  RoommateCandidateDaoImpl,
  UserDaoImpl,
  UserFlatDaoImpl,
  UserShowingDaoImpl
}
import ru.yandex.realty.rent.gen.RentModelsGen
import ru.yandex.realty.tracing.Traced

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait RentSpecBase
  extends MySQLTestContainer.V8_0
  with TestContainerDatasource
  with SlickOperations
  with ScalaFutures
  with Matchers
  with Checkers
  with ScalaCheckPropertyChecks
  with RentModelsGen
  with TracedLogging {
  this: Suite =>

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "realty_rent",
    databaseUser = "realty_rent",
    databasePassword = "realty_rent"
  )

  lazy val daoMetrics: DaoMetrics = DaoMetrics.stub()
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, containerConfig.databaseName)
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)
  lazy val masterSlaveDb: MasterSlaveJdbcDatabase = masterSlaveDb2.v1
  lazy val flatDbActions = new FlatDbActionsImpl()
  lazy val contractDbActions = new RentContractDbActionsImpl()
  lazy val paymentDbActions = new PaymentDbActionsImpl()
  lazy val paymentHistoryDbActions = new PaymentHistoryDbActionsImpl()
  lazy val userDbActions = new UserDbActionsImpl()
  lazy val ownerRequestDbActions: OwnerRequestDbActions = new OwnerRequestDbActionsImpl()
  lazy val houseServiceAuditDbActions: HouseServiceAuditDbActions = new HouseServiceAuditDbActionsImpl()
  lazy val meterReadingsDbActions: MeterReadingsDbActions = new MeterReadingsDbActionsImpl()
  lazy val periodDbActions: PeriodDbActions = new PeriodDbActionsImpl()
  lazy val houseServiceDbActions: HouseServiceDbActions = new HouseServiceDbActionsImpl()
  lazy val invalidPaymentNotificationDbActions: InvalidPaymentNotificationDbActions =
    new InvalidPaymentNotificationDbActionsImpl
  lazy val flatShowingDbActions = new FlatShowingDbActionsImpl()
  lazy val userShowingDbActions = new UserShowingDbActionsImpl()
  lazy val keysHandoverDbActions = new KeysHandoverDbActionsImpl()
  lazy val flatQuestionnaireDbActions = new FlatQuestionnaireDbActionsImpl()
  lazy val roommateCandidateDbActions = new RoommateCandidateDbActionsImpl()
  lazy val inventoryDbActions = new InventoryDbActionsImpl()
  lazy val statusAuditLogDbActions = new StatusAuditLogDbActionsImpl()

  lazy val inventoryDao = new InventoryDaoImpl(inventoryDbActions, flatShowingDbActions, masterSlaveDb2, daoMetrics)
  lazy val paymentDao = new PaymentDaoImpl(paymentDbActions, masterSlaveDb2, daoMetrics)
  lazy val paymentHistoryDao = new PaymentHistoryDaoImpl(paymentHistoryDbActions, masterSlaveDb2, daoMetrics)

  lazy val flatDao = new FlatDaoImpl(
    flatDbActions,
    contractDbActions,
    paymentDbActions,
    ownerRequestDbActions,
    periodDbActions,
    meterReadingsDbActions,
    houseServiceDbActions,
    flatShowingDbActions,
    keysHandoverDbActions,
    flatQuestionnaireDbActions,
    masterSlaveDb2,
    daoMetrics
  )
  lazy val houseServiceDao = new HouseServiceDaoImpl(
    houseServiceDbActions,
    ownerRequestDbActions,
    houseServiceAuditDbActions,
    masterSlaveDb2,
    daoMetrics
  )
  lazy val rentContractDao =
    new RentContractDaoImpl(
      contractDbActions,
      paymentDbActions,
      periodDbActions,
      userDbActions,
      flatShowingDbActions,
      masterSlaveDb2,
      daoMetrics
    )
  lazy val meterReadingsDao =
    new MeterReadingsDaoImpl(meterReadingsDbActions, periodDbActions, contractDbActions, masterSlaveDb)
  lazy val periodDao = new PeriodDaoImpl(periodDbActions, masterSlaveDb2, daoMetrics)
  lazy val ownerRequestDao: OwnerRequestDao =
    new OwnerRequestDaoImpl(
      ownerRequestDbActions,
      houseServiceDbActions,
      houseServiceAuditDbActions,
      masterSlaveDb2,
      daoMetrics
    )
  lazy val userFlatDbActions: UserFlatDbActions = new UserFlatDbActionsImpl()
  lazy val userDao: UserDao = new UserDaoImpl(userDbActions, masterSlaveDb2, daoMetrics)
  lazy val userFlatDao: UserFlatDao = new UserFlatDaoImpl(userFlatDbActions, masterSlaveDb2, daoMetrics)
  lazy val flatKeyCodeDbActions: FlatKeyCodeDbActions = new FlatKeyCodeDbActionsImpl()
  lazy val flatKeyCodeDao: FlatKeyCodeDao = new FlatKeyCodeDaoImpl(flatKeyCodeDbActions, masterSlaveDb2, daoMetrics)
  lazy val flatShowingDao =
    new FlatShowingDaoImpl(flatShowingDbActions, statusAuditLogDbActions, masterSlaveDb2, daoMetrics)
  lazy val userShowingDao = new UserShowingDaoImpl(userShowingDbActions, masterSlaveDb2, daoMetrics)
  lazy val roommateCandidateDao = new RoommateCandidateDaoImpl(roommateCandidateDbActions, masterSlaveDb2, daoMetrics)
  lazy val invalidPaymentNotificationsDao: InvalidPaymentNotificationsDao =
    new InvalidPaymentNotificationsDaoImpl(invalidPaymentNotificationDbActions, masterSlaveDb2, daoMetrics)

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))
}

trait CreateSchemaBeforeAll extends BeforeAndAfterAll {
  this: RentSpecBase with Suite =>

  override protected def beforeAll(): Unit = {
    database.run(script"sql/schema.sql").futureValue

    import jdbcProfile.api._

    val tables = database.run(sql"show tables".as[String]).futureValue.mkString("\n")
    log.info(s"""
          |CREATE SCHEMA:
          |--------------
          |$tables
          |--------------
          |""".stripMargin)(Traced.empty)
  }
}

trait CleanSchemaBeforeEach extends BeforeAndAfter {
  this: RentSpecBase with Suite =>

  before {
    database.run(script"sql/schema.sql").futureValue
  }

  after {
    database.run(script"sql/deleteTables.sql").futureValue
  }
}
