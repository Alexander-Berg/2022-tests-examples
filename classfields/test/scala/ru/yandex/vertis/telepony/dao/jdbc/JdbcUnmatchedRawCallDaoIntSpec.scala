package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.UnmatchedRawCallDaoSpec
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author evans
  */
class JdbcUnmatchedRawCallDaoIntSpec extends UnmatchedRawCallDaoSpec with JdbcSpecTemplate {

  override val dao: JdbcUnmatchedRawCallDao = new JdbcUnmatchedRawCallDao(dualDb)
}
