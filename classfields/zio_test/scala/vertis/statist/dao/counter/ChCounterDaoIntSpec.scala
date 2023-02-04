package vertis.statist.dao.counter

import org.joda.time.DateTime
import vertis.statist.db.Database
import vertis.statist.model._
import vertis.zio.BaseEnv
import zio.test.Assertion._
import zio.test._

/** @author zvez
  */
object ChCounterDaoIntSpec extends ChCounterDaoSpecBase {

  override def dao(db: Database): CounterDao = {
    val fieldTypes =
      Map("feed_id" -> FieldType.String, "categories" -> FieldType.Int64, "is_new" -> FieldType.Boolean)
    new ChCounterDao(db, "offer_event_counter_with_filter", true, AggregationFunction.Count, fieldTypes)
  }

  private val component = "card_view"
  private val day1 = DateTime.parse("2018-05-01")
  private val day2 = DateTime.parse("2018-05-02")

  override protected def daoTests(dao: CounterDao): Seq[ZSpec[BaseEnv, Any]] = Seq(
    suite("getCountWithFilter")(
      testM("filter fields") {
        assertM(
          dao.getMultipleByDayWithFilter(
            component,
            Set("2"),
            DatesPeriod(from = Some(day1)),
            Seq(
              FieldFilter.Eq("feed_id", FieldValue.StringValue("456")),
              FieldFilter.Eq("is_new", FieldValue.BooleanValue(true)),
              FieldFilter.Has("categories", FieldValue.Int64Value(1L))
            )
          )
        )(equalTo(DailyCounterValues(Map(day2.toLocalDate -> CounterValues(Map("2" -> 1))))))
      },
      testM("fail on unknown fields") {
        assertM(
          dao
            .getMultipleByDayWithFilter(
              component,
              Set("2"),
              DatesPeriod(from = Some(day1)),
              Seq(FieldFilter.Eq("offer_id", FieldValue.StringValue("456")))
            )
            .run
        )(fails(isSubtype[IllegalArgumentException](anything)))
      },
      testM("fail on mismatched field types") {
        assertM(
          dao
            .getMultipleByDayWithFilter(
              component,
              Set("2"),
              DatesPeriod(from = Some(day1)),
              Seq(FieldFilter.Eq("feed_id", FieldValue.Int64Value(456L)))
            )
            .run
        )(fails(isSubtype[IllegalArgumentException](anything)))
      }
    )
  )
}
