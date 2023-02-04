package ru.yandex.vertis.passport.dao.impl.mysql

import ru.yandex.vertis.passport.dao.{ApiTokenDao, ApiTokenDaoSpec}
import ru.yandex.vertis.passport.test.MySqlSupport
import ru.yandex.vertis.passport.util.mysql.DualDatabase

class MysqlApiTokenDaoSpec extends ApiTokenDaoSpec with MySqlSupport {
  import scala.concurrent.ExecutionContext.Implicits.global

  override val tokenDao: ApiTokenDao = new MysqlApiTokenDao(DualDatabase(dbs.passport))
}
