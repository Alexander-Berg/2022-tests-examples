package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.PromocodeAliasDaoSpec

/** Runnable specs on [[OrmPromocodeAliasDao]]
  *
  * @author alex-kovalenko
  */
class OrmPromocodeAliasDaoSpec extends PromocodeAliasDaoSpec with JdbcContainerSpecTemplate {

  val dao = new OrmPromocodeAliasDao(database)
}
