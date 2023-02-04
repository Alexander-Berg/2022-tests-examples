package ru.yandex.vertis.promocoder.dao.impl.jdbc

import ru.yandex.vertis.promocoder.dao.{
  AutoProlongFeatureDao,
  AutoProlongFeatureDaoSpec,
  CleanableDao,
  CleanableOrmAutoProlongFeatureDao
}

/** @author ruslansd
  */
class OrmAutoProlongFeatureDaoSpec extends AutoProlongFeatureDaoSpec with JdbcContainerSpecTemplate {

  override protected def dao: AutoProlongFeatureDao with CleanableDao =
    new OrmAutoProlongFeatureDao(database) with CleanableOrmAutoProlongFeatureDao
}
