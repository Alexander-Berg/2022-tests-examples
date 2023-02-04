package ru.yandex.realty.yankee.dao

import doobie.ConnectionIO
import org.scalatest.{BeforeAndAfter, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Millis, Minutes, Span}
import ru.yandex.realty.AsyncSpecBase
import ru.yandex.realty.db.testcontainers.{MySQLTestContainer, TestContainerDatasource}
import ru.yandex.realty.doobie.{DoobieTestDatabase, StubDbMonitorFactory}
import ru.yandex.realty.tracing.Traced
import ru.yandex.realty.watching2.doobie.{DoobieMysqlStageQueueDao, TestWatcherFactory}
import ru.yandex.realty.yankee.model.YangTask

trait BaseYankeeDaoSpec
  extends AsyncSpecBase
  with WordSpecLike
  with MySQLTestContainer.V8_0
  with TestContainerDatasource
  with BeforeAndAfter
  with ScalaFutures
  with DoobieTestDatabase {

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = Span(2, Minutes), interval = Span(20, Millis))

  implicit val trace: Traced = Traced.empty

  before {
    doobieDatabase.masterTransaction { implicit t =>
      executeSqlScript("sql/schema.sql")
        .flatMap(_ => executeSqlScript("sql/stage_queue_schema.sql"))
    }.futureValue
  }

  after {
    doobieDatabase.masterTransaction(_ => executeSqlScript("sql/drop_tables.sql")).futureValue
  }

  protected val databaseMonitorFactory = new StubDbMonitorFactory[ConnectionIO]
  private val stageQueueDao = new DoobieMysqlStageQueueDao(databaseMonitorFactory)
  private val watcherFactory = new TestWatcherFactory(stageQueueDao)
  private val yangTaskQueueManager = watcherFactory.buildQueueManager[Long](YangTask.EntityType)

  protected val yangTaskDao = new YangTaskDao(databaseMonitorFactory, yangTaskQueueManager)

  protected def getAllYangTasks(): Seq[YangTask] =
    doobieDatabase.replicaTransaction(yangTaskDao.getAll(_)).futureValue
}
