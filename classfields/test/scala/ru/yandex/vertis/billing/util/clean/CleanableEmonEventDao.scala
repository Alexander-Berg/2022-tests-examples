package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcEmonEventDao}
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

trait CleanableEmonEventDao extends JdbcEmonEventDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    database.runSyncUnit {
      sqlu"TRUNCATE TABLE emon_event"
    }
  }
}
