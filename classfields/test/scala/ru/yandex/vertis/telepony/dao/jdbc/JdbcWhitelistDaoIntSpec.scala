package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{WhitelistDao, WhitelistDaoSpec}
import ru.yandex.vertis.telepony.util.SharedDbSupport

/**
  * @author neron
  */
class JdbcWhitelistDaoIntSpec extends WhitelistDaoSpec with SharedDbSupport {

  override def dao: WhitelistDao = new JdbcWhitelistDao(sharedDualDb)
}
