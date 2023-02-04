package ru.yandex.vertis.promocoder.dao.impl.jvm

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.RoleDaoSpec

import scala.concurrent.ExecutionContext

/** Runnable spec on [[JvmRoleDao]]
  *
  * @author alex-kovalenko
  */
class JvmRoleDaoSpec extends RoleDaoSpec {
  import ExecutionContext.Implicits.global

  protected val roleDao = new JvmRoleDao
}
