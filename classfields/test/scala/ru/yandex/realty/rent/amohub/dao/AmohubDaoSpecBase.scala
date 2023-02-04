package ru.yandex.realty.rent.amohub.dao

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import org.scalatest.{BeforeAndAfter, Matchers, WordSpecLike}
import ru.yandex.realty.amohub.dao.actions.{ContactDbActions, ContactLeadDbActions, ContactPhoneDbActions}
import ru.yandex.realty.amohub.dao.actions.impl.{
  ContactDbActionsImpl,
  ContactLeadDbActionsImpl,
  ContactPhoneDbActionsImpl
}
import ru.yandex.realty.amohub.dao.impl.{ContactDaoImpl, ContactLeadDaoImpl}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.rent.amohub.dao.actions.impl._
import ru.yandex.realty.rent.amohub.dao.actions.LeadDbActions
import ru.yandex.realty.rent.amohub.dao.impl.{CrmActionDaoImpl, LeadDaoImpl}
import ru.yandex.realty.rent.amohub.gen.AmohubModelsGen
import ru.yandex.realty.tracing.Traced

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait AmohubDaoSpecBase
  extends MySQLTestContainer.V8_0
  with TestContainerDatasource
  with WordSpecLike
  with BeforeAndAfter
  with SlickOperations
  with ScalaFutures
  with Matchers
  with Checkers
  with PropertyChecks
  with AmohubModelsGen {

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "realty_amohub",
    databaseUser = "realty_amohub",
    databasePassword = "realty_amohub"
  )

  lazy val daoMetrics: DaoMetrics = DaoMetrics.stub()
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, containerConfig.databaseName)
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)
  lazy val crmActionDao: CrmActionDao = new CrmActionDaoImpl(new CrmActionDbActionsImpl(), masterSlaveDb2, daoMetrics)
  lazy val contactDbActions: ContactDbActions = new ContactDbActionsImpl
  lazy val contactPhoneDbActions: ContactPhoneDbActions = new ContactPhoneDbActionsImpl
  lazy val leadDbActions: LeadDbActions = new LeadDbActionsImpl
  lazy val contactLeadDbActions: ContactLeadDbActions = new ContactLeadDbActionsImpl
  lazy val contactDao = new ContactDaoImpl(contactDbActions, contactPhoneDbActions, masterSlaveDb2, daoMetrics)
  lazy val leadDao = new LeadDaoImpl(leadDbActions, contactLeadDbActions, masterSlaveDb2, daoMetrics)
  lazy val contactLeadDao = new ContactLeadDaoImpl(contactLeadDbActions, masterSlaveDb2, daoMetrics)

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global
  implicit val traced: Traced = Traced.empty

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  before {
    database.run(script"sql/schema.sql").futureValue
  }

  after {
    database.run(script"sql/delete_tables.sql").futureValue
  }
}
