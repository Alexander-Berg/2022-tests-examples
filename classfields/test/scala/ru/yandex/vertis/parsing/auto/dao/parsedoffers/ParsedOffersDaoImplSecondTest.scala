package ru.yandex.vertis.parsing.auto.dao.parsedoffers

import org.joda.time.DateTime
import org.jooq.Record
import org.jooq.impl.DSL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.CommonModel
import ru.yandex.vertis.parsing.auto.components.TestCatalogsAndFeaturesComponents
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.{TParsedOffers, TParsedOffersRegions}
import ru.yandex.vertis.parsing.auto.dao.model.{ParsedRow, QueryParams}
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.common.{AscDesc, Site}
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.dao.watchers.FieldWatcher
import ru.yandex.vertis.parsing.diffs.DiffAnalyzerFactory
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.dao.{Shard, TestShard}
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.validators.FilterReason
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class ParsedOffersDaoImplSecondTest extends FunSuite with MockitoSupport {
  private val o: TParsedOffers = TParsedOffers.T_PARSED_OFFERS

  private val r: TParsedOffersRegions = TParsedOffersRegions.T_PARSED_OFFERS_REGIONS

  private val components = TestCatalogsAndFeaturesComponents

  private val rTree = components.regionTree

  private val mockedTimeService = mock[TimeService]

  private val parsedOffersDao = new ParsedOffersDaoImpl {
    override def watchers: Seq[FieldWatcher[_ <: Record, _, ParsedRow]] = Seq()

    override def parsingShard: Shard = TestShard

    override def diffAnalyzerFactory: DiffAnalyzerFactory[ParsedRow] = ???

    override def timeService: TimeService = mockedTimeService
  }

  implicit private val trace: Traced = TracedUtils.empty

  test("getOrSetSendingForCallCenter: no exclude sites") {
    TestShard.master.expectQuery(
      "select `parsing`.`t_parsed_offers`.`id`, `parsing`.`t_parsed_offers`.`hash`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`status`, " +
        "`parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`url`, `parsing`.`t_parsed_offers`.`data`, " +
        "`parsing`.`t_parsed_offers`.`create_date`, `parsing`.`t_parsed_offers`.`update_date`, " +
        "`parsing`.`t_parsed_offers`.`sent_date`, `parsing`.`t_parsed_offers`.`open_date`, " +
        "`parsing`.`t_parsed_offers`.`source`, `parsing`.`t_parsed_offers`.`callcenter`, " +
        "`parsing`.`t_parsed_offers`.`offer_id`, `parsing`.`t_parsed_offers`.`version`, " +
        "`parsing`.`t_parsed_offers`.`status_update_date`, " +
        "`parsing`.`t_parsed_offers`.`deactivate_date` " +
        "from `parsing`.`t_parsed_offers` " +
        "where (`parsing`.`t_parsed_offers`.`status` = ? and `parsing`.`t_parsed_offers`.`callcenter` = ? " +
        "and `parsing`.`t_parsed_offers`.`category` = ?) limit ?",
      Seq("SENDING", "callCenter", "CARS", Int.box(10))
    )
    TestShard.master.respondWithEmptyTableResult(o)
    TestShard.master.expectQuery(
      "select `parsing`.`t_parsed_offers`.`id`, `parsing`.`t_parsed_offers`.`hash`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`status`, " +
        "`parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`url`, `parsing`.`t_parsed_offers`.`data`, " +
        "`parsing`.`t_parsed_offers`.`create_date`, `parsing`.`t_parsed_offers`.`update_date`, " +
        "`parsing`.`t_parsed_offers`.`sent_date`, `parsing`.`t_parsed_offers`.`open_date`, " +
        "`parsing`.`t_parsed_offers`.`source`, `parsing`.`t_parsed_offers`.`callcenter`, " +
        "`parsing`.`t_parsed_offers`.`offer_id`, `parsing`.`t_parsed_offers`.`version`, " +
        "`parsing`.`t_parsed_offers`.`status_update_date`, " +
        "`parsing`.`t_parsed_offers`.`deactivate_date` " +
        "from `parsing`.`t_parsed_offers` " +
        "where (`parsing`.`t_parsed_offers`.`status` = ? and `parsing`.`t_parsed_offers`.`callcenter` = ? " +
        "and `parsing`.`t_parsed_offers`.`category` = ?) limit ?",
      Seq("READY", "callCenter", "CARS", Int.box(10))
    )
    TestShard.master.respondWithEmptyTableResult(o)
    val result = parsedOffersDao.getOrSetSendingForCallCenter("callCenter", Category.CARS, 0, 10, Seq.empty)
    assert(result.isEmpty)
  }

  test("getOrSetSendingForCallCenter: with exclude sites") {
    TestShard.master.expectQuery(
      "select `parsing`.`t_parsed_offers`.`id`, `parsing`.`t_parsed_offers`.`hash`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`status`, " +
        "`parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`url`, `parsing`.`t_parsed_offers`.`data`, " +
        "`parsing`.`t_parsed_offers`.`create_date`, `parsing`.`t_parsed_offers`.`update_date`, " +
        "`parsing`.`t_parsed_offers`.`sent_date`, `parsing`.`t_parsed_offers`.`open_date`, " +
        "`parsing`.`t_parsed_offers`.`source`, `parsing`.`t_parsed_offers`.`callcenter`, " +
        "`parsing`.`t_parsed_offers`.`offer_id`, `parsing`.`t_parsed_offers`.`version`, " +
        "`parsing`.`t_parsed_offers`.`status_update_date`, " +
        "`parsing`.`t_parsed_offers`.`deactivate_date` " +
        "from `parsing`.`t_parsed_offers` " +
        "where (`parsing`.`t_parsed_offers`.`status` = ? and `parsing`.`t_parsed_offers`.`callcenter` = ? " +
        "and `parsing`.`t_parsed_offers`.`category` = ?) limit ?",
      Seq("SENDING", "callCenter", "CARS", Int.box(10))
    )
    TestShard.master.respondWithEmptyTableResult(o)
    TestShard.master.expectQuery(
      "select `parsing`.`t_parsed_offers`.`id`, `parsing`.`t_parsed_offers`.`hash`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`status`, " +
        "`parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`url`, `parsing`.`t_parsed_offers`.`data`, " +
        "`parsing`.`t_parsed_offers`.`create_date`, `parsing`.`t_parsed_offers`.`update_date`, " +
        "`parsing`.`t_parsed_offers`.`sent_date`, `parsing`.`t_parsed_offers`.`open_date`, " +
        "`parsing`.`t_parsed_offers`.`source`, `parsing`.`t_parsed_offers`.`callcenter`, " +
        "`parsing`.`t_parsed_offers`.`offer_id`, `parsing`.`t_parsed_offers`.`version`, " +
        "`parsing`.`t_parsed_offers`.`status_update_date`, " +
        "`parsing`.`t_parsed_offers`.`deactivate_date` " +
        "from `parsing`.`t_parsed_offers` " +
        "where (`parsing`.`t_parsed_offers`.`status` = ? and `parsing`.`t_parsed_offers`.`callcenter` = ? " +
        "and `parsing`.`t_parsed_offers`.`category` = ? " +
        "and `parsing`.`t_parsed_offers`.`site` not in (?)) limit ?",
      Seq("READY", "callCenter", "CARS", "drom", Int.box(10))
    )
    TestShard.master.respondWithEmptyTableResult(o)
    val result = parsedOffersDao.getOrSetSendingForCallCenter("callCenter", Category.CARS, 0, 10, Seq(Site.Drom))
    assert(result.isEmpty)
  }

  test("refilterProcessLater") {
    val date = new DateTime(2019, 1, 15, 10, 0, 0, 0)
    val sqlDate = DateUtils.date2sqlTs(date)
    TestShard.master.expectQuery(
      "select distinct `parsing`.`t_parsed_offers`.`hash` " +
        "from `parsing`.`t_parsed_offers` join `parsing`.`t_parsed_offers_filtered_reasons` " +
        "on `parsing`.`t_parsed_offers`.`hash` = `parsing`.`t_parsed_offers_filtered_reasons`.`hash` " +
        "where (`parsing`.`t_parsed_offers`.`status` in (?) and `parsing`.`t_parsed_offers`.`create_date` >= ? " +
        "and `parsing`.`t_parsed_offers`.`category` in (?) " +
        "and `parsing`.`t_parsed_offers_filtered_reasons`.`v_value` in (?)) " +
        "limit ?",
      Seq("FILTERED", sqlDate, "CARS", "process_later", Int.box(1000))
    )
    TestShard.master.respondWithEmptyTableResult(o)
    parsedOffersDao.refilter(
      QueryParams(
        status = Seq(CommonModel.Status.FILTERED),
        createDateGte = Some(date),
        filterReasons = Seq(FilterReason.ProcessLater),
        category = Seq(Category.CARS),
        limit = Some(1000)
      )
    )
  }

  test("countProcessLater") {
    val date = new DateTime(2019, 1, 15, 10, 0, 0, 0)
    val sqlDate = DateUtils.date2sqlTs(date)
    TestShard.slave.expectQuery(
      "select count(distinct `parsing`.`t_parsed_offers`.`hash`) " +
        "from `parsing`.`t_parsed_offers` join `parsing`.`t_parsed_offers_filtered_reasons` " +
        "on `parsing`.`t_parsed_offers`.`hash` = `parsing`.`t_parsed_offers_filtered_reasons`.`hash` " +
        "where (`parsing`.`t_parsed_offers`.`status` in (?) and `parsing`.`t_parsed_offers`.`create_date` >= ? " +
        "and `parsing`.`t_parsed_offers`.`category` in (?) " +
        "and `parsing`.`t_parsed_offers_filtered_reasons`.`v_value` in (?)) " +
        "limit ?",
      Seq("FILTERED", sqlDate, "CARS", "process_later", Int.box(1000))
    )
    TestShard.slave.respondWithSingleQueryResult(DSL.field("count", classOf[Int]))(Int.box(15))
    val count = parsedOffersDao.count(
      QueryParams(
        status = Seq(CommonModel.Status.FILTERED),
        createDateGte = Some(date),
        filterReasons = Seq(FilterReason.ProcessLater),
        category = Seq(Category.CARS),
        limit = Some(1000)
      )
    )
    assert(count == 15)
  }

  test("getCurrentDaySentOrReadyWithCallCenters") {
    val createDate = new DateTime(2019, 1, 30, 9, 0, 0, 0)
    val date = new DateTime(2019, 1, 30, 0, 0, 0, 0)
    val sqlDate = DateUtils.date2sqlTs(date)
    val url = TestDataUtils.testAvitoCarsUrl
    val row = TestDataUtils.testRow(url, category = Category.CARS, geobaseId = 11)
    when(mockedTimeService.getNow).thenReturn(date)
    val sql = "select `parsing`.`t_parsed_offers`.`site`, " +
      "`parsing`.`t_parsed_offers`.`callcenter`, " +
      "`parsing`.`t_parsed_offers_regions`.`v_value`, " +
      "count(`parsing`.`t_parsed_offers`.`id`) " +
      "from `parsing`.`t_parsed_offers` " +
      "join `parsing`.`t_parsed_offers_regions` " +
      "on `parsing`.`t_parsed_offers`.`hash` = `parsing`.`t_parsed_offers_regions`.`hash` " +
      "where (`parsing`.`t_parsed_offers`.`callcenter` is not null " +
      "and `parsing`.`t_parsed_offers`.`category` = ? " +
      "and (`parsing`.`t_parsed_offers`.`sent_date` >= ? or `parsing`.`t_parsed_offers`.`status` in (?, ?))) " +
      "group by `parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`callcenter`, " +
      "`parsing`.`t_parsed_offers_regions`.`v_value`"
    val args = Seq("CARS", sqlDate, "READY", "SENDING")
    TestShard.slave.expectQuery(sql, args)
    TestShard.slave.respondWithSingleQueryResult(o.SITE, o.CALLCENTER, r.V_VALUE, DSL.field("count", classOf[Int]))(
      Site.Avito.name,
      "callcenter",
      Long.box(10776),
      Int.box(1)
    )
    assert(
      parsedOffersDao.getCurrentDaySentOrReadyWithCallCenters(Category.CARS) ==
        Map((Site.Avito, "callcenter", 10776) -> 1)
    )
  }

  test("getActiveHashes") {
    val sql = "select `parsing`.`t_parsed_offers`.`id`, `parsing`.`t_parsed_offers`.`hash`, " +
      "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`status`, " +
      "`parsing`.`t_parsed_offers`.`site`, `parsing`.`t_parsed_offers`.`url`, " +
      "`parsing`.`t_parsed_offers`.`data`, `parsing`.`t_parsed_offers`.`create_date`, " +
      "`parsing`.`t_parsed_offers`.`update_date`, `parsing`.`t_parsed_offers`.`sent_date`, " +
      "`parsing`.`t_parsed_offers`.`open_date`, `parsing`.`t_parsed_offers`.`source`, " +
      "`parsing`.`t_parsed_offers`.`callcenter`, `parsing`.`t_parsed_offers`.`offer_id`, " +
      "`parsing`.`t_parsed_offers`.`version`, `parsing`.`t_parsed_offers`.`status_update_date`, " +
      "`parsing`.`t_parsed_offers`.`deactivate_date` from `parsing`.`t_parsed_offers` " +
      "where (`parsing`.`t_parsed_offers`.`id` >= ? and `parsing`.`t_parsed_offers`.`site` in (?) " +
      "and `parsing`.`t_parsed_offers`.`category` in (?) and `parsing`.`t_parsed_offers`.`deactivate_date` is null) " +
      "order by `parsing`.`t_parsed_offers`.`id` asc limit ?"
    val args = Seq(BigDecimal.valueOf(500).bigDecimal, "avito", "CARS", Int.box(1000))
    TestShard.slave.expectQuery(sql, args)
    TestShard.slave.respondWithEmptyQueryResult(o.ID, o.URL, o.HASH)
    parsedOffersDao.getParsedOffersByParams(
      "active_hashes",
      QueryParams(
        site = Seq(Site.Avito),
        category = Seq(Category.CARS),
        idGte = Some(500),
        isActive = Some(true),
        orderById = Some(AscDesc.Asc),
        limit = Some(1000)
      )
    )
  }

  test("getReadyWithoutCallCenter") {
    val sql = "select `parsing`.`t_parsed_offers`.`hash`, " +
      "`parsing`.`t_parsed_offers`.`site`, " +
      "`parsing`.`t_parsed_offers_regions`.`v_value` " +
      "from `parsing`.`t_parsed_offers` " +
      "join `parsing`.`t_parsed_offers_regions` " +
      "on `parsing`.`t_parsed_offers`.`hash` = `parsing`.`t_parsed_offers_regions`.`hash` " +
      "where (" +
      "`parsing`.`t_parsed_offers`.`status` = ? " +
      "and (`parsing`.`t_parsed_offers`.`callcenter` is null " +
      "or `parsing`.`t_parsed_offers`.`callcenter` not in (?, ?)) " +
      "and `parsing`.`t_parsed_offers`.`category` = ?) " +
      "order by case `parsing`.`t_parsed_offers`.`site` " +
      "when ? then ? " +
      "when ? then ? " +
      "when ? then ? " +
      "when ? then ? " +
      "else ? end asc " +
      "limit ?"
    val args = Seq(
      "READY",
      "cc1",
      "cc2",
      "CARS",
      "avito",
      Int.box(0),
      "drom",
      Int.box(1),
      "am.ru",
      Int.box(2),
      "youla",
      Int.box(3),
      Int.box(4),
      Int.box(1000)
    )
    TestShard.slave.expectQuery(sql, args)
    TestShard.slave.respondWithEmptyTableResult(o)
    parsedOffersDao.getReadyWithoutCallCenter(Category.CARS, 1000, Seq("cc1", "cc2"))
  }
}
