package ru.yandex.vertis.promocoder.dao.impl.jdbc

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.RoleDaoSpec

/** Runnable specs on [[OrmRoleDao]]
  *
  * @author alex-kovalenko
  */
class OrmRoleDaoSpec extends RoleDaoSpec with JdbcContainerSpecTemplate {

  protected val roleDao = new OrmRoleDao(database)
}
