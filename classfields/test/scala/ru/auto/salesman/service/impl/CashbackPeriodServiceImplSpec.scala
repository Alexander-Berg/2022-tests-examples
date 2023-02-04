package ru.auto.salesman.service.impl

import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException
import org.joda.time.DateTime
import ru.auto.salesman.dao.CashbackPeriodDao
import ru.auto.salesman.environment.IsoDateFormatter
import ru.auto.salesman.model.PeriodId
import ru.auto.salesman.model.cashback.CashbackPeriod
import ru.auto.salesman.service.CashbackPeriodService.{
  DateNotInPast,
  DuplicatedPeriod,
  StartAfterFinish
}
import ru.auto.salesman.service.ReactorService
import ru.auto.salesman.test.BaseSpec

class CashbackPeriodServiceImplSpec extends BaseSpec {
  private val reactorService = mock[ReactorService]
  private val cashbackPeriodDao = mock[CashbackPeriodDao]

  private val service =
    new CashbackPeriodServiceImpl(cashbackPeriodDao, reactorService)

  private val start = DateTime.now()
  private val finish = DateTime.now()
  "CashbackPeriodService" should {

    "trigger period moderation" in {

      val newPeriod = CashbackPeriod(
        id = PeriodId(3),
        start = start.minusDays(2),
        finish = finish.minusDays(1),
        isActive = true,
        previousPeriod = Some(PeriodId(3))
      )

      val alignedStart = newPeriod.start.withTimeAtStartOfDay()
      val alignedFinish = newPeriod.finish.withTimeAtStartOfDay()

      (cashbackPeriodDao.insert _)
        .expects(alignedStart, alignedFinish)
        .returningZ(())

      val newPeriodAligned =
        newPeriod.copy(
          start = alignedStart,
          finish = alignedFinish
        )

      (cashbackPeriodDao.getByInterval _)
        .expects(alignedStart, alignedFinish)
        .returningZ(Some(newPeriodAligned))

      (reactorService.triggerCashbackPeriodTaskReaction _)
        .expects(PeriodId(3), IsoDateFormatter.print(alignedFinish))
        .returningZ(())

      service
        .triggerModeration(
          newPeriod.start,
          newPeriod.finish
        )
        .success
        .value shouldBe newPeriodAligned
    }

    "fail on dates from future" in {
      val s = DateTime.now()
      service
        .triggerModeration(s, s)
        .failure
        .cause
        .squash shouldBe DateNotInPast(s)
    }

    "fail if start after finish" in {
      val a = DateTime.now().minusDays(1)
      val b = DateTime.now().minusDays(2)
      service
        .triggerModeration(a, b)
        .failure
        .cause
        .squash shouldBe StartAfterFinish(
        a.withTimeAtStartOfDay(),
        b.withTimeAtStartOfDay()
      )
    }

    "fail on duplicated period" in {
      val period =
        CashbackPeriod(
          id = PeriodId(1),
          start = start.minusDays(2),
          finish = finish.minusDays(1),
          isActive = false,
          previousPeriod = None
        )

      (cashbackPeriodDao.insert _)
        .expects(
          period.start.withTimeAtStartOfDay(),
          period.finish.withTimeAtStartOfDay()
        )
        .throwingZ(new MySQLIntegrityConstraintViolationException)

      service
        .triggerModeration(
          period.start,
          period.finish
        )
        .failure
        .cause
        .squash shouldBe DuplicatedPeriod(
        period.start.withTimeAtStartOfDay(),
        period.finish.withTimeAtStartOfDay()
      )
    }

    "return period by id" in {
      val period =
        CashbackPeriod(
          id = PeriodId(1),
          start = start.minusDays(2),
          finish = finish.minusDays(1),
          isActive = true,
          previousPeriod = None
        )

      (cashbackPeriodDao.getById _)
        .expects(PeriodId(1))
        .returningZ(Some(period))

      (cashbackPeriodDao.getById _)
        .expects(PeriodId(2))
        .returningZ(None)

      service.getById(PeriodId(1)).success.value should contain(period)
      service.getById(PeriodId(2)).success.value shouldBe empty
    }

    "close period by id" in {

      (cashbackPeriodDao.closeById _)
        .expects(PeriodId(1))
        .returningZ(1)

      service.closeById(PeriodId(1)).success.value
    }
  }
}
