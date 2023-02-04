package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcNotifyClientDao}
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

/**
  * @author tolmach
  */
trait CleanableNotifyClientDao extends JdbcNotifyClientDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    database.runSyncUnit {
      sqlu"DELETE FROM balance_notify_client"
    }
  }
}
