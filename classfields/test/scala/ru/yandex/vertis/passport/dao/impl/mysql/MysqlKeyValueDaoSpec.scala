package ru.yandex.vertis.passport.dao.impl.mysql

import ru.yandex.vertis.passport.dao.{KeyValueDao, KeyValueDaoSpec}
import ru.yandex.vertis.passport.test.MySqlSupport
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
class MysqlKeyValueDaoSpec extends KeyValueDaoSpec with MySqlSupport {
  override val dao: KeyValueDao = new MysqlKeyValueDao(DualDatabase(dbs.passport))
}
