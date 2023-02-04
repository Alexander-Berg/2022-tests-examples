package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.dao.{HoboDao, HoboDaoSpec}

/**
  * @author ruslansd
  */
class JdbcHoboDaoSpec extends HoboDaoSpec {
  override protected def hoboDao: HoboDao = new JdbcHoboDao(campaignEventDatabase)
}
