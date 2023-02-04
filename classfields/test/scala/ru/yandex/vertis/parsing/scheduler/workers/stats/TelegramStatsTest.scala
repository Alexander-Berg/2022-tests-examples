package ru.yandex.vertis.parsing.scheduler.workers.stats

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.concurrent.ScalaFutures._
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.components.clients.MockedClientsSupport
import ru.yandex.vertis.parsing.auto.components.dao.MockedDaoSupport
import ru.yandex.vertis.parsing.components.time.TimeSupport
import ru.yandex.vertis.parsing.scheduler.workers.stats.telegram.{TelegramCarStats, TelegramTruckStats}
import ru.yandex.vertis.parsing.util.DateUtils._
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.table.TableData
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.parsing.util.StringUtils.RichString

import scala.concurrent.Future

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class TelegramStatsTest extends FunSuite with MockitoSupport {

  private val telegramStats = new TelegramTruckStats
    with TelegramCarStats
    with MockedClientsSupport
    with MockedDaoSupport
    with TimeSupport

  private val mockedStatsDao = telegramStats.statsDao

  private val mockedTelegramClient = telegramStats.telegramClient

  implicit private val trace: Traced = TracedUtils.empty

  test("sendSentStats handle no value for some date") {
    when(mockedStatsDao.getTelegramSentStats(eq(Category.TRUCKS))(?)).thenReturn(
      Seq(
        ("avito", "2018-05-10".parseDateAs("yyyy-MM-dd"), 64),
        ("avito", "2018-05-16".parseDateAs("yyyy-MM-dd"), 20),
        ("avito", "2018-05-17".parseDateAs("yyyy-MM-dd"), 55),
        ("drom", "2018-05-10".parseDateAs("yyyy-MM-dd"), 32),
        ("drom", "2018-05-17".parseDateAs("yyyy-MM-dd"), 7)
      )
    )
    var tableData: TableData = null
    stub(mockedTelegramClient.table(_: String, _: TableData)(_: Traced)) {
      case (_, table, _) =>
        tableData = table
        Future.unit
    }
    implicit val t: Traced = Traced.empty
    telegramStats.sendTrucksSentStats(t).futureValue
    assert(
      tableData == TableData(
        "Комтранс. Поступило данных",
        List("Сайт", "2018-05-10", "2018-05-16", "2018-05-17"),
        Seq(
          List("avito", "64", "20", "55"),
          List("drom", "32", "0", "7")
        )
      )
    )
  }

  test("sendSentStats for cars") {
    when(mockedStatsDao.getTelegramSentStats(eq(Category.CARS))(?)).thenReturn(
      Seq(
        ("avito", "2018-05-10".parseDateAs("yyyy-MM-dd"), 64),
        ("avito", "2018-05-16".parseDateAs("yyyy-MM-dd"), 20),
        ("avito", "2018-05-17".parseDateAs("yyyy-MM-dd"), 55),
        ("drom", "2018-05-10".parseDateAs("yyyy-MM-dd"), 32),
        ("drom", "2018-05-17".parseDateAs("yyyy-MM-dd"), 7)
      )
    )
    var tableData: TableData = null
    stub(mockedTelegramClient.table(_: String, _: TableData)(_: Traced)) {
      case (_, table, _) =>
        tableData = table
        Future.unit
    }
    implicit val t: Traced = Traced.empty
    telegramStats.sendCarsSentStats(t).futureValue
    assert(
      tableData == TableData(
        "Легковые. Поступило данных",
        List("Сайт", "2018-05-10", "2018-05-16", "2018-05-17"),
        Seq(
          List("avito", "64", "20", "55"),
          List("drom", "32", "0", "7")
        )
      )
    )
  }
}
