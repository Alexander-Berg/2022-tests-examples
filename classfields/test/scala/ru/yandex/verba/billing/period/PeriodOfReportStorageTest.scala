package ru.yandex.verba.billing.period

import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import ru.yandex.verba.billing.model.PeriodOfReport
import ru.yandex.verba.billing.storage.PeriodOfReportStorage
import ru.yandex.verba.core.application.DBInitializer

import java.time.{OffsetDateTime, ZoneOffset}

/**
 * Created by slewa
 * Date: 15.07.14
 * Time: 14:41
 * mailto: slewa@yandex-team.ru
 */
@Ignore
class PeriodOfReportStorageTest extends AnyFreeSpec with Matchers {

  DBInitializer

  "Adding new billing period and deleting it should work" in {
    val start = OffsetDateTime.of(1988, 10, 24, 13, 0, 0, 0, ZoneOffset.ofHours(3))
    val end = OffsetDateTime.of(1988, 11, 25, 12, 0, 0, 0, ZoneOffset.ofHours(3))
    val reportPeriod: PeriodOfReport = PeriodOfReportStorage.ref.addPeriod("test", 1988, start, end).get
    reportPeriod.name shouldEqual "test"
    reportPeriod.year shouldEqual 1988
    PeriodOfReportStorage.ref.deletePeriod("test", 1988)
    PeriodOfReportStorage.ref.getPeriod("test", 1988) shouldEqual None
  }

}
