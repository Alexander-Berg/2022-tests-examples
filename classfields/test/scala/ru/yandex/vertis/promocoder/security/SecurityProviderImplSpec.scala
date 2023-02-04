package ru.yandex.vertis.promocoder.security

import org.junit.runner.RunWith

import ru.yandex.vertis.promocoder.dao.impl.jvm.JvmRoleDao
import ru.yandex.vertis.promocoder.service.RoleService.Roles
import ru.yandex.vertis.promocoder.service.impl.RoleServiceImpl

/** Runnable specs om [[SecurityProviderImpl]]
  *
  * @author alex-kovalenko
  */
class SecurityProviderImplSpec extends SecurityProviderSpec {

  import scala.concurrent.ExecutionContext.Implicits.global

  val rolesDao = new JvmRoleDao()
  rolesDao.set(superUser, Roles.SuperUser)
  val roles = new RoleServiceImpl(rolesDao)

  val securityProvider: SecurityProvider =
    new SecurityProviderImpl(roles)
}
