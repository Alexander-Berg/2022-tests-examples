package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.CampaignEventDaoSpec

/**
  * Runnable spec on [[JdbcCampaignEventDao]]
  *
  * @author dimas
  */
class JdbcCampaignEventDaoSpec extends CampaignEventDaoSpec with JdbcSpecTemplate {

  protected val campaignEventDao = new JdbcCampaignEventDao(campaignEventDatabase)

}
