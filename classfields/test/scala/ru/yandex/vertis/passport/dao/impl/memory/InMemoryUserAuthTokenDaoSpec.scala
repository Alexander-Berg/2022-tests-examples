package ru.yandex.vertis.passport.dao.impl.memory

import ru.yandex.vertis.passport.dao.{UserAuthTokenDao, UserAuthTokenDaoSpec}

class InMemoryUserAuthTokenDaoSpec extends UserAuthTokenDaoSpec {
  override val tokenDao: UserAuthTokenDao = new InMemoryUserAuthTokenDao
}
