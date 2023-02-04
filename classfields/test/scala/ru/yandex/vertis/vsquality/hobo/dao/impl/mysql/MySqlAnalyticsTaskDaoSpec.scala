package ru.yandex.vertis.vsquality.hobo.dao.impl.mysql

import ru.yandex.vertis.vsquality.hobo.dao.{AnalyticsTaskDao, AnalyticsTaskDaoSpecBase}
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[MySqlAnalyticsTaskDao]]
  *
  * @author semkagtn
  */

class MySqlAnalyticsTaskDaoSpec extends AnalyticsTaskDaoSpecBase with MySqlSpecBase {

  override val analyticsTaskDao: AnalyticsTaskDao = new MySqlAnalyticsTaskDao(database, upsertBatchSize = 1)
}
