package ru.yandex.vertis.telepony.util

import org.scalatest.{BeforeAndAfterAll, Suite}
import org.scalatest.concurrent.ScalaFutures
import ru.yandex.vertis.telepony.dao.jdbc.JdbcSpecUtils
import ru.yandex.vertis.telepony.dao.jdbc.api._
import ru.yandex.vertis.telepony.util.db.{DualDatabase, PlainDualDatabase}

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.io.Source

/**
  * @author zvez
  */
trait SharedDbSupport extends BeforeAndAfterAll with ScalaFutures with TestPrometheusComponent {
  this: Suite =>

  lazy val sharedDatabase: PlainDualDatabase = {
    val d = SharedDbSupport.sharedDualDb
    afterAllHook = () => clearDatabase(d.master.db)
    d
  }

  lazy val sharedDualDb: DualDatabase =
    sharedDatabase.named("shared_db_test")(prometheusRegistry)

  @volatile private var afterAllHook = () => ()

  protected def clearSharedDatabase() = {
    afterAllHook()
  }

  private def clearDatabase(d: Database): Unit = {
    Await.result(d.run(JdbcSpecUtils.getSchemaAction(SharedDbSupport.clearScript)), 5.seconds)
  }

  override protected def afterAll(): Unit = {
    afterAllHook()
    super.afterAll()
  }

}

object SharedDbSupport extends JdbcBuilder {

  def schemaScript: String = {
    val schemaPath = "/sql/shared/telepony.sql"
    Source.fromInputStream(getClass.getResourceAsStream(schemaPath)).mkString
  }

  val clearScript: String =
    Source.fromInputStream(getClass.getResourceAsStream("/sql/shared/drop.sql")).mkString

  //should be lazy due strange immediate instantiation of all test in maven-surefire-plugin
  lazy val sharedDualDb: PlainDualDatabase = {
    sys.addShutdownHook {
      sharedDualDb.master.db.close()
      sharedDualDb.slave.db.close()
    }
    createSimpleDualDatabase()
  }

}
