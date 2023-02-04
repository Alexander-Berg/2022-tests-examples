package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{CallDaoV2Spec, HistoryRedirectDaoV2}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author evans
  */
class JdbcCallDaoV2IntSpec extends CallDaoV2Spec with JdbcSpecTemplate {

  private val domain: TypedDomains.Value = TypedDomains.autoru_def

  override val numberDao = new JdbcOperatorNumberDaoV2(domain)

  val historyRedirectDaoV2 = new JdbcHistoryRedirectDaoV2(domain)

  override val redirectDao =
    new JdbcRedirectDaoV2(TypedDomains.autoru_def) with HistoryRedirectDaoAdapter {
      override def historyRedirectDao: HistoryRedirectDaoV2 = historyRedirectDaoV2
    }

  override val callDao = new JdbcCallDaoV2(domain)
}
