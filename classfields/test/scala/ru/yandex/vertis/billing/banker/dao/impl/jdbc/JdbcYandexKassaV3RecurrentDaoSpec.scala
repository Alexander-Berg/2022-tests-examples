package ru.yandex.vertis.billing.banker.dao.impl.jdbc

import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.YandexKassaV3RecurrentDaoSpec
import ru.yandex.vertis.billing.banker.dao.util.CleanableJdbcYandexKassaV3RecurrentDao

/**
  * @author ruslansd
  */
class JdbcYandexKassaV3RecurrentDaoSpec extends YandexKassaV3RecurrentDaoSpec with JdbcSpecTemplate {

  override protected def recurrents: CleanableJdbcYandexKassaV3RecurrentDao =
    new JdbcYandexKassaV3RecurrentDao(database) with CleanableJdbcYandexKassaV3RecurrentDao

}
