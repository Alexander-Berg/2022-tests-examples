package vertis.statist.dao.counter

import org.joda.time.DateTime
import vertis.statist.db.{Database, StatistChSpec}
import vertis.statist.model._
import vertis.zio.BaseEnv
import zio.test.Assertion._
import zio.test.{assertM, ZSpec}
import zio.{Has, ZIO}

/** @author reimai
  */
abstract class ChCounterDaoSpecBase extends StatistChSpec {

  private val cardViewComponent = "card_view"
  private val phoneShowComponent = "phone_show"
  private val components = Set(cardViewComponent, phoneShowComponent)
  private val offerId = "2"
  private val offerId2 = "3"
  private val day1 = DateTime.parse("2018-05-01")
  private val day2 = DateTime.parse("2018-05-02")

  protected def dao(db: Database): CounterDao

  override def chSpec: ZSpec[BaseEnv with Has[Database], Any] =
    suiteM(s"counter tests for ${getClass.getSimpleName.stripSuffix("$")}") {
      ZIO.service[Database].map(dao).map { dao =>
        daoTests(dao)
      }
    }

  protected def daoTests(dao: CounterDao): Seq[ZSpec[BaseEnv, Any]] =
    Seq(
      suite("getCount")(
        testM("return zero if no events") {
          assertM(dao.get(cardViewComponent, "1"))(equalTo(0))
        },
        testM("return something") {
          assertM(dao.get(cardViewComponent, offerId))(isPositive)
        },
        testM("use from") {
          for {
            zeroInFuture <- assertM(
              dao
                .get(cardViewComponent, offerId, DatesPeriod(from = Some(DateTime.now.plusDays(1))))
            )(equalTo(0))
            firstDayCount <- assertM(dao.get(cardViewComponent, offerId, DatesPeriod(from = Some(day1))))(equalTo(6))
            secondDayCount <- assertM(dao.get(cardViewComponent, offerId, DatesPeriod(from = Some(day2))))(equalTo(1))
          } yield zeroInFuture && firstDayCount && secondDayCount
        },
        testM("use until") {
          for {
            sixOverall <- assertM(dao.get(cardViewComponent, offerId, DatesPeriod(until = Some(DateTime.now))))(
              equalTo(6)
            )
            zeroUntilFirstDay <- assertM(
              dao.get(cardViewComponent, offerId, DatesPeriod(until = Some(day1.minusDays(10))))
            )(equalTo(0))
          } yield sixOverall && zeroUntilFirstDay
        }
      ),
      suite("getMultiple")(
        testM("return zero result if no events") {
          assertM(dao.getMultiple(cardViewComponent, Set("1")).map(_.values))(equalTo(Map("1" -> 0)))
        },
        testM("return something") {
          assertM(dao.getMultiple(cardViewComponent, Set(offerId)).map(_.values))(isNonEmpty)
        },
        testM("work with multiple values") {
          assertM(dao.getMultiple(cardViewComponent, Set(offerId, offerId2)).map(_.values)) {
            isNonEmpty &&
            hasKey[Id, Int](offerId, equalTo(6)) &&
            hasKey[Id, Int](offerId2, equalTo(8))
          }
        },
        testM("use from/until") {
          assertM(
            dao
              .getMultiple(
                cardViewComponent,
                Set(offerId, offerId2),
                DatesPeriod(Some(day1.minusDays(10)), Some(day2))
              )
              .map(_.values)
          )(
            isNonEmpty &&
              hasKey[Id, Int](offerId, equalTo(5)) &&
              hasKey[Id, Int](offerId2, equalTo(0))
          )
        }
      ),
      suite("getMultipleComponents")(
        testM("return zero result if no events") {
          val expected = MultipleCountersValues(
            Map(
              "42" -> ObjectCounterValues(
                Map(cardViewComponent -> 0, phoneShowComponent -> 0)
              )
            )
          )
          assertM(dao.getMultipleComponents(components, Set("42")))(equalTo(expected))
        },
        testM("return right values") {
          assertM(dao.getMultipleComponents(components, Set(offerId, offerId2)).map(_.byObject))(
            equalTo(
              Map(
                offerId ->
                  ObjectCounterValues(Map(cardViewComponent -> 6, phoneShowComponent -> 5)),
                offerId2 ->
                  ObjectCounterValues(Map(cardViewComponent -> 8, phoneShowComponent -> 0))
              )
            )
          )
        },
        testM("use from/until") {
          assertM(
            dao
              .getMultipleComponents(
                components,
                Set(offerId, offerId2),
                DatesPeriod(Some(day1.minusDays(10)), Some(day2))
              )
              .map(_.byObject)
          )(
            equalTo(
              Map(
                offerId -> ObjectCounterValues(Map(cardViewComponent -> 5, phoneShowComponent -> 5)),
                offerId2 -> ObjectCounterValues(Map(cardViewComponent -> 0, phoneShowComponent -> 0))
              )
            )
          )
        }
      ),
      suite("getMultipleComponentsByDay")(
        testM("return zero result if no events") {
          val id = "43"
          assertM(dao.getMultipleComponentsByDay(components, Set(id), DatesPeriod.Open).map(_.byObject))(
            equalTo(Map(id -> ObjectDailyValues(Map.empty)))
          )
        },
        testM("return counters") {
          val d1 = day1.toLocalDate
          val d2 = day2.toLocalDate

          assertM(
            dao.getMultipleComponentsByDay(components, Set(offerId, offerId2), DatesPeriod.Open).map(_.byObject)
          ) {
            equalTo(
              Map(
                offerId -> ObjectDailyValues(
                  Map(
                    d1 -> ObjectCounterValues(Map(cardViewComponent -> 5, phoneShowComponent -> 5)),
                    d2 -> ObjectCounterValues(Map(cardViewComponent -> 1, phoneShowComponent -> 0))
                  )
                ),
                offerId2 -> ObjectDailyValues(
                  Map(d2 -> ObjectCounterValues(Map(cardViewComponent -> 8, phoneShowComponent -> 0)))
                )
              )
            )
          }
        }
      ),
      suite("getPlainByDayWithFilter")(
        testM("return zero result if no events") {
          val now = DateTime.now
          assertM(
            dao.getPlainByDayWithFilter(cardViewComponent, DatesPeriod(Some(now)), Seq.empty[FieldFilter]).map(_.days)
          )(
            isEmpty
          )
        },
        testM("return counters") {
          val d1 = day1.toLocalDate
          val d2 = day2.toLocalDate

          assertM(dao.getPlainByDayWithFilter(cardViewComponent, DatesPeriod.Open, Seq.empty[FieldFilter]).map(_.days))(
            equalTo(
              Map(
                d1 -> ObjectCounterValues(Map(cardViewComponent -> 5)),
                d2 -> ObjectCounterValues(Map(cardViewComponent -> 9))
              )
            )
          )
        }
      )
    )
}
