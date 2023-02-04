package ru.yandex.vertis.telepony.dao.jdbc

import ru.yandex.vertis.telepony.dao._
import ru.yandex.vertis.telepony.model.{TypedDomain, TypedDomains}
import ru.yandex.vertis.telepony.util.JdbcSpecTemplate

/**
  * @author neron
  */
class JdbcCommonCallDaoIntSpec extends CommonCallDaoSpec with JdbcSpecTemplate {

  override def domain: TypedDomain = TypedDomains.autoru_def

  override def dao: CommonCallDao = new JdbcCommonCallDao(dualDb, domain)

  override def appCallDao: AppCallDao = new JdbcAppCallDao(dualDb)

  override def redirectDao: RedirectDaoV2 = new JdbcRedirectDaoV2(domain) with HistoryRedirectDaoAdapter {
    override lazy val historyRedirectDao: HistoryRedirectDaoV2 = new JdbcHistoryRedirectDaoV2(domain)
  }

  override def numberDao: OperatorNumberDaoV2 = new JdbcOperatorNumberDaoV2(domain)

  override def callDao: CallDaoV2 = new JdbcCallDaoV2(domain)

  override def callbackOrderDao: CallbackOrderDao = new JdbcCallbackOrderDao(dualDb)

  override def callbackDao: CallbackCallDao = new JdbcCallbackCallDao(dualDb)

  override def appBackCallDao: AppBackCallDao = new JdbcAppBackCallDao(dualDb)

}
