package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.joda.time.DateTime
import ru.yandex.vertis.promocoder.dao.impl.jdbc.config.dualdb.PlainDualDatabase
import ru.yandex.vertis.promocoder.dao.{CleanableOrmFeatureInstanceDao, FeatureInstanceDaoSpec}
import ru.yandex.vertis.promocoder.util.TimeService

/** Runnable specs on [[OrmFeatureInstanceDao]]
  *
  * @author alex-kovalenko
  */
class OrmFeatureInstanceDaoSpec extends FeatureInstanceDaoSpec with JdbcContainerSpecTemplate {

  val timeService = new TimeService {
    override def now(): DateTime = StableNow
  }

  val dao =
    new OrmFeatureInstanceDao(PlainDualDatabase(database, database), timeService)
      with CleanableOrmFeatureInstanceDao[OrmFeatureInstanceDao.FeatureInstances]

}
