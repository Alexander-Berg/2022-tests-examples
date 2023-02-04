package vertis.statist.dao.counter

import org.joda.time.{DateTime, LocalDate}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import vertis.statist.dao.counter.aggregation.Aggregation
import vertis.statist.dao.counter.aggregation.model.AggregationKey.AggToday
import vertis.statist.dao.counter.aggregation.model.ComponentsReq
import vertis.statist.model._

/**
 * @author Ratskevich Natalia reimai@yandex-team.ru
 */
class AggregationSpec extends AnyWordSpec with Matchers {

  private val agg = new Aggregation()
  private def today() = DatesPeriod(Some(DateTime.now().withTimeAtStartOfDay()))

  "AggregatingChCounterDao" should {
    "aggregate by today" in {
      agg.getKey(today()) shouldBe Some(AggToday)
    }

    "merge requests" in {
      val fstPeriod = DatesPeriod(Some(DateTime.parse("2022-01-02")), Some(DateTime.parse("2022-02-01")))
      val sndPeriod = DatesPeriod(None, Some(DateTime.parse("2021-04-30")))
      val fst = ComponentsReq(Set("message", "call"), Set("id1", "id2"), fstPeriod)
      val snd = ComponentsReq(Set("view"), Set("id3", "id2"), sndPeriod)
      agg.merge(Seq(fst, snd)) shouldBe
        ComponentsReq(
          Set("message", "call", "view"),
          Set("id1", "id2", "id3"),
          DatesPeriod(None, Some(DateTime.parse("2022-02-01")))
        )
    }

    "extract results" in {
      val result = agg.extractResult(
        MultipleDailyValues(
          Map(
            "id1" -> ObjectDailyValues(
              Map(
                // before the period
                LocalDate.parse("2022-01-01") -> ObjectCounterValues(Map("view" -> 5, "call" -> 1)),
                LocalDate.parse("2022-01-02") -> ObjectCounterValues(Map("view" -> 6, "call" -> 2)),
                LocalDate.parse("2022-01-31") -> ObjectCounterValues(Map("view" -> 10, "call" -> 3)),
                // until bound, should be excluded
                LocalDate.parse("2022-02-01") -> ObjectCounterValues(
                  Map("view" -> 100500, "call" -> 2000, "message" -> 3000)
                )
              )
            ),
            "id3" -> ObjectDailyValues(Map())
          )
        ),
        ComponentsReq(
          Set("message", "call"),
          Set("id1", "id2"),
          DatesPeriod(Some(DateTime.parse("2022-01-02")), Some(DateTime.parse("2022-02-01")))
        )
      )
      val expectedResult = MultipleCountersValues(
        Map(
          "id1" -> ObjectCounterValues(Map("call" -> 5, "message" -> 0)),
          "id2" -> ObjectCounterValues(Map("call" -> 0, "message" -> 0))
        )
      )
      result shouldBe expectedResult
    }
  }
}
