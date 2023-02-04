package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{HistoryRedirectDaoSpec, HistoryRedirectDaoV2}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author evans
  */
class JdbcHistoryRedirectDaoV2IntSpec extends HistoryRedirectDaoSpec with JdbcSpecTemplate {

  override def historyRedirectDao: HistoryRedirectDaoV2 = new JdbcHistoryRedirectDaoV2(TypedDomains.autoru_def)
}
