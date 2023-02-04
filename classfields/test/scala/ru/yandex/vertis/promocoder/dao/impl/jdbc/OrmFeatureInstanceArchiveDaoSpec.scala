package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.impl.jdbc.config.dualdb.PlainDualDatabase
import ru.yandex.vertis.promocoder.dao.{CleanableOrmFeatureInstanceDao, FeatureInstanceArchiveDaoSpec}

/** @author alex-kovalenko
  */
class OrmFeatureInstanceArchiveDaoSpec extends FeatureInstanceArchiveDaoSpec with JdbcContainerSpecTemplate {

  override val dao =
    new OrmFeatureInstanceArchiveDao(PlainDualDatabase(database, database))
      with CleanableOrmFeatureInstanceDao[OrmFeatureInstanceArchiveDao.ArchiveFeatureInstances]
}
