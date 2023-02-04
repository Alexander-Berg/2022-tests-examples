package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcCallFactDao}
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

/**
  * @author tolmach
  */
trait CleanableCallFactDao extends JdbcCallFactDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    dualDatabase.master.runSyncUnit {
      sqlu"DELETE FROM callfact"
    }
  }

}
