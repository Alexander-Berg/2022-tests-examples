package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.ArchiveDaoSpec
import ru.yandex.vertis.billing.util.clean.CleanableArchiveDao

/**
  * Runnable specs on [[JdbcArchiveDao]]
  *
  * @author Alexander Kovalenko (alex-kovalenko@yandex-team.ru)
  */
class JdbcArchiveDaoSpec extends ArchiveDaoSpec with JdbcSpecTemplate {

  val archiveDao: JdbcArchiveDao with CleanableArchiveDao = new JdbcArchiveDao(archiveDualDatabase)
    with CleanableArchiveDao

}
