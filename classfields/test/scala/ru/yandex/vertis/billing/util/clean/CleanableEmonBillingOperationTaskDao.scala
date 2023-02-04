package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcEmonBillingOperationTaskDao}
import slick.jdbc.MySQLProfile.api._
import scala.util.Try

trait CleanableEmonBillingOperationTaskDao extends JdbcEmonBillingOperationTaskDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    database.runSyncUnit {
      sqlu"TRUNCATE TABLE emon_billing_operation_task"
    }
  }
}
