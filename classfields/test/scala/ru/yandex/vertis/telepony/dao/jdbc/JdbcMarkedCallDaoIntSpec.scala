package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{MarkedCallDao, MarkedCallDaoSpec}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author ruslansd
  */
class JdbcMarkedCallDaoIntSpec extends MarkedCallDaoSpec with JdbcSpecTemplate {

  override def markedCallDao: MarkedCallDao = new JdbcMarkedCallDao(dualDb)
}
