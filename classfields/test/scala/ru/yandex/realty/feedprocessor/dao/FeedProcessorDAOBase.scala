package ru.yandex.realty.feedprocessor.dao

import org.scalatest.{BeforeAndAfter, Matchers, Suite}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.{Checkers, PropertyChecks}
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.application.ng.db.MasterSlaveJdbcDatabase2
import ru.yandex.realty.db.testcontainers.TestContainer.ContainerConfig
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, SlickOperations, TestContainerDatasource}
import ru.yandex.realty.feedprocessor.dao.actions.{PartnerFeedDbActions, PartnerOfferDbActions}
import ru.yandex.realty.feedprocessor.dao.impl.{PartnerFeedDAOImpl, PartnerOfferDAOImpl}
import ru.yandex.realty.ops.DaoMetrics
import ru.yandex.realty.picapica.MdsUrlBuilder

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait FeedProcessorDAOBase
  extends MySQLTestContainer.V8_0
  with TestContainerDatasource
  with SlickOperations
  with ScalaFutures
  with Matchers
  with Checkers
  with PropertyChecks {
  this: Suite =>

  override lazy val containerConfig: ContainerConfig = ContainerConfig(
    databasePort = 3306,
    databaseName = "realty_feed_processor",
    databaseUser = "realty_feed_processor",
    databasePassword = "realty_feed_processor"
  )

  implicit val ex: ExecutionContextExecutor = ExecutionContext.global
  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  lazy val daoMetrics: DaoMetrics = DaoMetrics.stub()
  lazy val database2: MasterSlaveJdbcDatabase2.DbWithProperties =
    MasterSlaveJdbcDatabase2.DbWithProperties(database, containerConfig.databaseName)
  lazy val masterSlaveDb2: MasterSlaveJdbcDatabase2 = MasterSlaveJdbcDatabase2(database2, database2)
  implicit lazy val mdsUrlBuilder = new MdsUrlBuilder("//avatars.mdst.yandex.net")
  lazy val partnerFeedDbActions = new PartnerFeedDbActions()
  lazy val partnerOfferDbActions = new PartnerOfferDbActions()
  lazy val partnerFeedDAOImpl =
    new PartnerFeedDAOImpl(partnerFeedDbActions, partnerOfferDbActions, masterSlaveDb2, daoMetrics)
  lazy val partnerOfferDAOImpl = new PartnerOfferDAOImpl(partnerOfferDbActions, masterSlaveDb2, daoMetrics)

}

trait CleanSchemaBeforeEach extends BeforeAndAfter {
  this: FeedProcessorDAOBase with Suite =>

  before {
    database.run(script"sql/schema.sql").futureValue
  }

  after {
    database.run(script"sql/deleteTables.sql").futureValue
  }
}
