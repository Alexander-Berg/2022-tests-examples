package ru.yandex.vertis.billing.util.clean

import ru.yandex.vertis.billing.dao.impl.jdbc.{DatabaseOps, JdbcArchiveDao}
import ru.yandex.vertis.billing.service.ArchiveService.{RecordType, RecordTypes}
import ru.yandex.vertis.billing.util.clean.CleanableArchiveDao.buildDeleteQuery
import slick.dbio.DBIO
import slick.jdbc.MySQLProfile.api._

import scala.util.Try

trait CleanableArchiveDao extends JdbcArchiveDao with CleanableDao {

  override def clean(): Try[Unit] = Try {
    RecordTypes.values.foreach { recordType =>
      database.master.runSync {
        buildDeleteQuery(recordType)
      }
    }
  }

}

object CleanableArchiveDao {

  private def deleteArchiveBaseQuery(dbName: String): DBIO[Int] = {
    sqlu"""DELETE FROM #$dbName"""
  }

  private def buildDeleteQuery(rt: RecordType): DBIO[Int] = {
    val dbName = rt match {
      case RecordTypes.Campaign =>
        JdbcArchiveDao.CampaignArchiveDatabaseName
      case RecordTypes.Binding =>
        JdbcArchiveDao.BindingArchiveDatabaseName
      case RecordTypes.Limit =>
        JdbcArchiveDao.LimitsArchiveDatabaseName
    }
    deleteArchiveBaseQuery(dbName)
  }

}
