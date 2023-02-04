package ru.yandex.vertis.vsquality.hobo.dao.impl.mysql

import org.scalacheck.Gen

import ru.yandex.vertis.vsquality.hobo.dao.{KeyValueDao, KeyValueDaoSpecBase}
import ru.yandex.vertis.vsquality.hobo.model.SummarySalaryStatistics
import ru.yandex.vertis.vsquality.hobo.model.generators.CoreGenerators
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[MySqlSummarySalaryStatisticsKeyValueDao]]
  *
  * @author semkagtn
  */

class MySqlSummarySalaryStatisticsKeyValueDaoSpec
  extends KeyValueDaoSpecBase[SummarySalaryStatistics]
  with MySqlSpecBase {

  override val valueGen: Gen[SummarySalaryStatistics] = CoreGenerators.SummarySalaryStatisticsGen

  override val keyValueDao: KeyValueDao[SummarySalaryStatistics] = new MySqlSummarySalaryStatisticsKeyValueDao(database)
}
