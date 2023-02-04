package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao._

class JdbcCampaignCallDaoSpec extends CampaignCallDaoSpec with JdbcSpecTemplate {

  protected lazy val campaignCallDao = new JdbcCampaignCallDao(campaignEventDatabase)

  protected lazy val callDao = new JdbcCallFactDao(campaignEventDualDatabase)

}
