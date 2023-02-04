package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{HistoryRedirectDaoV2, OperatorNumberDaoV2, RedirectDaoV2, RedirectDaoV2Spec}
import ru.yandex.vertis.telepony.model.{TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author @logab
  */
class JdbcRedirectDaoV2IntSpec extends RedirectDaoV2Spec with JdbcSpecTemplate {

  private val domain: TypedDomain = TypedDomains.autoru_def

  override val redirectDao: RedirectDaoV2 = new JdbcRedirectDaoV2(domain) with HistoryRedirectDaoAdapter {

    override val historyRedirectDao: HistoryRedirectDaoV2 = new JdbcHistoryRedirectDaoV2(domain)
  }

  override val operatorNumberDao: OperatorNumberDaoV2 = new JdbcOperatorNumberDaoV2(domain)

}
