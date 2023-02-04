package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.MtsPhoneDaoSpec
import ru.yandex.vertis.telepony.util.SharedDbSupport

/**
  * @author evans
  */
class JdbcMtsPhoneDaoIntSpec extends MtsPhoneDaoSpec with SharedDbSupport {

  override val dao = new JdbcMtsPhoneDao(sharedDualDb)
}
