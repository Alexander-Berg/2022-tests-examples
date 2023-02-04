package ru.yandex.verba.billing

import akka.util.Timeout
import org.scalatest.Ignore
import org.scalatest.freespec.AnyFreeSpec
import ru.yandex.verba.billing.model.{ModificationSummary, WorkDay}
import ru.yandex.verba.billing.service.impl.{ExcelPresentation, TreeStylePresentation}
import ru.yandex.verba.billing.service.{ReportManager, WorkManager}
import ru.yandex.verba.billing.storage.WorkStorage
import ru.yandex.verba.core.application._
import ru.yandex.verba.core.util.{DateUtils, JsonUtils, VerbaUtils}
import ru.yandex.verba.sec.user
import spray.json._

import java.io.FileOutputStream
import java.time.OffsetDateTime
import java.time.temporal.{ChronoField, ChronoUnit}
import scala.concurrent.duration._
import scala.language.implicitConversions

/**
 * Author: Evgeny Vanslov (evans@yandex-team.ru)
 * Created: 17.07.14
 */
@Ignore
class BillingTest extends AnyFreeSpec with VerbaUtils {
  DBInitializer
  val workStorage = WorkStorage.ref
  val workManager = WorkManager.ref
  val userStorage = user.storage
  val reportManager = ReportManager.ref
  implicit val timeout: FiniteDuration = 10.hours
  implicit val tout = Timeout(timeout)
  implicit val ec = system.dispatcher

  "Try calculate all days between last day and today" ignore {
    val lastDate = workStorage.getLastDay.truncatedTo(ChronoUnit.DAYS)
    val todayMidnight = OffsetDateTime
      .now()
      .truncatedTo(ChronoUnit.DAYS)
      .`with`(ChronoField.MILLI_OF_SECOND, 1) // 1 ms after midnight for correct comparison
    val days =
      LazyList.from(1).map(days => todayMidnight.minus(days, ChronoUnit.DAYS)).takeWhile(!lastDate.isAfter(_)).toList
    days.foreach { day =>
      logger.debug(s"Calculating work for day $day")
      val work = workManager.calculateWork(day).await match {
        case Seq() => Seq(WorkDay(userStorage.get(0L), day, Seq.empty, ModificationSummary.empty))
        case w => w
      }
      logger.debug(s"Calculated work:\n${work.mkString("\n")}")
    }
  }

  implicit def sprayPimp[T](any: T)(implicit writer: JsonWriter[T]) = enrichAny(any).toJson(jsonWriter)

  "General billing info" ignore {
    val creator = new reportManager.PeriodContextCreator(periodIds = Seq(46), userIds = Seq())
    val x: JsValue = reportManager.calculateReport(creator, TreeStylePresentation).await
    println(JsonUtils.toJson(x))
  }

  "Billing info on exact day (end of period)" ignore {
    val creator = new reportManager.DatesContextCreator(
      fromDate = DateUtils.fromDateString("26.08.2014"),
      toDate = DateUtils.fromDateString("26.08.2014"),
      userIds = Seq(),
      billingServiceIds = Seq()
    )
    val x: JsValue = reportManager.calculateReport(creator, TreeStylePresentation).await
    println(JsonUtils.toJson(x))
  }

  "Excel billing info" in {

    val creator = new reportManager.DatesContextCreator(
      OffsetDateTime.parse("2015-12-02T00:00+03:00"),
      OffsetDateTime.parse("2015-12-03T00:00+03:00"),
      userIds = Seq(1120000000027035L, 1120000000036867L),
      Seq(7841340L)
    )
//    val creator = new reportManager.PeriodContextCreator(periodIds = Seq(10), userIds = Seq(1120000000008842l))
    val x = reportManager.calculateReport(creator, ExcelPresentation).await
    using(new FileOutputStream("/home/mioto/xls/billing.xls")) { out =>
      x.write(out);
    }
  }

}
