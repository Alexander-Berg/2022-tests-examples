package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcCampaignHistoryDao}
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

trait CleanableCampaignHistoryDao extends JdbcCampaignHistoryDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    database.runSyncUnit {
      sqlu"DELETE FROM campaign_history"
    }
  }

}
