package ru.yandex.vertis.vsquality.hobo.service.impl.mysql

import ru.yandex.vertis.vsquality.hobo.dao.impl.mysql.MySqlSummarySalaryStatisticsKeyValueDao
import ru.yandex.vertis.vsquality.hobo.service.impl.SummarySalaryStatisticsServiceImpl
import ru.yandex.vertis.vsquality.hobo.service.{SummarySalaryStatisticsService, SummarySalaryStatisticsServiceSpecBase}
import ru.yandex.vertis.vsquality.hobo.util.MySqlSpecBase

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Specs on [[SummarySalaryStatisticsServiceImpl]]
  *
  * @author semkagtn
  */

class MySqlSummarySalaryStatisticsServiceImplSpec extends SummarySalaryStatisticsServiceSpecBase with MySqlSpecBase {

  override val summarySalaryStatisticsService: SummarySalaryStatisticsService =
    new SummarySalaryStatisticsServiceImpl(new MySqlSummarySalaryStatisticsKeyValueDao(database))
}
