package ru.yandex.vertis.passport.dao.impl.mysql

import ru.yandex.vertis.passport.dao.UserModerationStatusDaoSpec
import ru.yandex.vertis.passport.test.MySqlSupport
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class MysqlUserModerationStatusDaoSpec extends UserModerationStatusDaoSpec with MySqlSupport {
  override val dao = new MysqlUserModerationStatusDao(DualDatabase(dbs.passport))
}
