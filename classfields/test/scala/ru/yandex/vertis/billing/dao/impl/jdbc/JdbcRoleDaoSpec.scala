package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{RoleDao, RoleDaoSpec}

/**
  * Runnable spec on [[JdbcRoleDao]]
  */
class JdbcRoleDaoSpec extends RoleDaoSpec with JdbcSpecTemplate {

  protected val roleDao: RoleDao = new JdbcRoleDao(billingDualDatabase)

}
