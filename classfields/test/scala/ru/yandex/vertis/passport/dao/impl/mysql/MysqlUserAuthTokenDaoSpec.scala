package ru.yandex.vertis.passport.dao.impl.mysql

import ru.yandex.vertis.passport.dao.{UserAuthTokenDao, UserAuthTokenDaoSpec}
import ru.yandex.vertis.passport.test.MySqlSupport
import ru.yandex.vertis.passport.util.mysql.DualDatabase

class MysqlUserAuthTokenDaoSpec extends UserAuthTokenDaoSpec with MySqlSupport {
  override def tokenDao: UserAuthTokenDao = new MysqlUserAuthTokenDao(DualDatabase(dbs.passport))
}
