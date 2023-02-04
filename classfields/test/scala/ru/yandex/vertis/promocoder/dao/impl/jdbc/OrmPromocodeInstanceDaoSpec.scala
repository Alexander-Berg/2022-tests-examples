package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.{CleanableOrmPromocodeInstanceDao, PromocodeInstanceDaoSpec}

/** Runnable specs on [[OrmPromocodeInstanceDao]]
  *
  * @author alex-kovalenko
  */
class OrmPromocodeInstanceDaoSpec extends PromocodeInstanceDaoSpec with JdbcContainerSpecTemplate {

  val promocodeDao = new OrmPromocodeDao(database)

  val dao = new OrmPromocodeInstanceDao(database) with CleanableOrmPromocodeInstanceDao
}
