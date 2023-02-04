package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao.{HistoryRedirectDaoV2, HoboCallCheckTaskDao, HoboCallCheckTaskDaoSpec}
import ru.yandex.vertis.telepony.model.TypedDomains
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author ponydec
  */
class JdbcHoboCallCheckTaskDaoIntSpec extends HoboCallCheckTaskDaoSpec with JdbcSpecTemplate {

  override def hoboCallCheckTaskDao: HoboCallCheckTaskDao = new JdbcHoboCallCheckTaskDao(dualDb)

  private val domain: TypedDomains.Value = TypedDomains.autoru_def

  override val numberDao = new JdbcOperatorNumberDaoV2(domain)

  val historyRedirectDaoV2 = new JdbcHistoryRedirectDaoV2(domain)

  override val redirectDao =
    new JdbcRedirectDaoV2(domain) with HistoryRedirectDaoAdapter {
      override def historyRedirectDao: HistoryRedirectDaoV2 = historyRedirectDaoV2
    }

  override val callDao = new JdbcCallDaoV2(domain)
}
