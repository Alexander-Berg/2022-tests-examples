package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{JdbcCallCenterCallDao, _}
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

trait CleanableCallCenterCallDao extends JdbcCallCenterCallDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    database.runSyncUnit {
      sqlu"DELETE FROM call_center_call"
    }
  }

}
