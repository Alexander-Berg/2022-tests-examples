package vertis.statist.service

import vertis.statist.dao.TestCounterDao
import vertis.statist.dao.TestCounterDao._
import vertis.statist.model._
import vertis.statist.service.counter.CounterServiceImpl
import vertis.zio.test.ZioSpecBase

/** @author zvez
  */
class CounterServiceImplSpec extends ZioSpecBase {

  private val service = new CounterServiceImpl(TestCounterDao.default)

  "CounterServiceImpl" when {
    "call getComposite" should {
      "return result for two periods" in ioTest {
        val expectedResult =
          CounterCompositeValues(
            Map(
              IdA -> CounterCompositeValue(3, 2),
              IdB -> CounterCompositeValue(4, 4)
            )
          )
        service.getCompositeMultiple(CardShow, Set(IdA, IdB), None).map(_ shouldBe expectedResult)
      }

      "use from" in ioTest {
        val expectedResult =
          CounterCompositeValues(
            Map(
              IdA -> CounterCompositeValue(2, 2),
              IdB -> CounterCompositeValue(4, 4)
            )
          )
        service
          .getCompositeMultiple(
            CardShow,
            Set(IdA, IdB),
            Some(today.toDateTimeAtStartOfDay)
          )
          .map(_ shouldBe expectedResult)
      }
    }

    "call getMultipleByDay" should {
      "use getMultipleComponentsByDay" in ioTest {
        val expectedResult =
          MultipleDailyValues(
            Map(
              IdA -> ObjectDailyValues(
                Map(
                  yesterday ->
                    ObjectCounterValues(Map(CardShow -> 1, PhoneShow -> 0)),
                  today ->
                    ObjectCounterValues(Map(CardShow -> 2, PhoneShow -> 0))
                )
              ),
              IdB -> ObjectDailyValues(
                Map(
                  yesterday -> ObjectCounterValues(Map(CardShow -> 0, PhoneShow -> 0)),
                  today -> ObjectCounterValues(Map(CardShow -> 4, PhoneShow -> 1))
                )
              )
            )
          )

        service
          .getMultipleComponentsByDay(
            Set(CardShow, PhoneShow),
            Set(IdA, IdB),
            DatesPeriod.Open
          )
          .map(_ shouldBe expectedResult)
      }
    }
  }
}
