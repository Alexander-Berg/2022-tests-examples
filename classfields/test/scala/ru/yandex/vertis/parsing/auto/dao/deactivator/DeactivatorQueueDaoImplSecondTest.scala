package ru.yandex.vertis.parsing.auto.dao.deactivator

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.{TDeactivatorQueue, TParsedOffers}
import ru.yandex.vertis.parsing.common.Site
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.dao.deactivator.{ExternalOfferCheckResult, ExternalOfferStatus}
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.dao.TestShard
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DeactivatorQueueDaoImplSecondTest extends FunSuite with MockitoSupport {
  private val mockedTimeService = mock[TimeService]

  when(mockedTimeService.getNow).thenReturn(new DateTime(2019, 4, 17, 0, 0, 0, 0))

  private val deactivatorQueueDao = new DeactivatorQueueDaoImpl(TestShard, mockedTimeService)

  implicit private val trace: Traced = TracedUtils.empty

  private val d = TDeactivatorQueue.T_DEACTIVATOR_QUEUE
  private val o = TParsedOffers.T_PARSED_OFFERS

  test("add") {
    TestShard.master.expectBatchQuery(
      Seq(
        "insert ignore into `parsing`.`t_deactivator_queue` (`hash`, `added`) " +
          "values ('hash1', {ts '2019-04-17 00:00:00.0'})",
        "insert ignore into `parsing`.`t_deactivator_queue` (`hash`, `added`) " +
          "values ('hash2', {ts '2019-04-17 00:00:00.0'})"
      )
    )
    TestShard.master.respondWithUpdateResult(Seq(true, false))
    val result = deactivatorQueueDao.add(Seq("hash1", "hash2"))
    assert(result("hash1"))
    assert(!result("hash2"))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("getUnprocessed") {
    TestShard.master.expectQuery(
      "select `parsing`.`t_deactivator_queue`.`id`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`site`, " +
        "`parsing`.`t_deactivator_queue`.`hash`, `parsing`.`t_parsed_offers`.`url` " +
        "from `parsing`.`t_deactivator_queue` join `parsing`.`t_parsed_offers` " +
        "on `parsing`.`t_deactivator_queue`.`hash` = `parsing`.`t_parsed_offers`.`hash` " +
        "where (`parsing`.`t_deactivator_queue`.`processed` = ? " +
        "and `parsing`.`t_parsed_offers`.`site` = ? " +
        "and `parsing`.`t_parsed_offers`.`category` = ?) " +
        "limit ?",
      Seq(DateUtils.date2sqlTs(new DateTime(1970, 1, 1, 0, 0, 0, 0)), "drom", "CARS", Int.box(10))
    )
    TestShard.master.respondWithEmptyQueryResult(d.ID, o.CATEGORY, o.SITE, d.HASH, o.URL)
    deactivatorQueueDao.getUnprocessed(Site.Drom, Some(Category.CARS), 10)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("getUnprocessedForReactivation: idGte") {
    TestShard.master.expectQuery(
      "select `parsing`.`t_deactivator_queue`.`id`, " +
        "`parsing`.`t_parsed_offers`.`category`, `parsing`.`t_parsed_offers`.`site`, " +
        "`parsing`.`t_deactivator_queue`.`hash`, `parsing`.`t_parsed_offers`.`url` " +
        "from `parsing`.`t_deactivator_queue` join `parsing`.`t_parsed_offers` " +
        "on `parsing`.`t_deactivator_queue`.`hash` = `parsing`.`t_parsed_offers`.`hash` " +
        "where (`parsing`.`t_deactivator_queue`.`processed` = ? " +
        "and `parsing`.`t_deactivator_queue`.`id` >= ?) " +
        "order by `parsing`.`t_deactivator_queue`.`id` asc " +
        "limit ?",
      Seq(DateUtils.date2sqlTs(new DateTime(1970, 1, 1, 0, 0, 0, 0)), Int.box(1), Int.box(10))
    )
    TestShard.master.respondWithEmptyQueryResult(d.ID, o.CATEGORY, o.SITE, d.HASH, o.URL)
    deactivatorQueueDao.getUnprocessedForReactivation(10, idGte = 1)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("setProcessed") {
    TestShard.master.expectBatchQuery(
      Seq(
        "update `parsing`.`t_deactivator_queue` " +
          "set `parsing`.`t_deactivator_queue`.`processed` = {ts '2019-04-17 00:00:00.0'}, " +
          "`parsing`.`t_deactivator_queue`.`deactivated` = 0 " +
          "where (`parsing`.`t_deactivator_queue`.`hash` = 'hash1' " +
          "and `parsing`.`t_deactivator_queue`.`processed` = {ts '1970-01-01 00:00:00.0'})",
        "update `parsing`.`t_deactivator_queue` " +
          "set `parsing`.`t_deactivator_queue`.`processed` = {ts '2019-04-17 00:00:00.0'}, " +
          "`parsing`.`t_deactivator_queue`.`deactivated` = 1 " +
          "where (`parsing`.`t_deactivator_queue`.`hash` = 'hash2' " +
          "and `parsing`.`t_deactivator_queue`.`processed` = {ts '1970-01-01 00:00:00.0'})",
        "update `parsing`.`t_deactivator_queue` " +
          "set `parsing`.`t_deactivator_queue`.`processed` = {ts '2019-04-17 00:00:00.0'}, " +
          "`parsing`.`t_deactivator_queue`.`deactivated` = 0 " +
          "where (`parsing`.`t_deactivator_queue`.`hash` = 'hash3' " +
          "and `parsing`.`t_deactivator_queue`.`processed` = {ts '1970-01-01 00:00:00.0'})"
      )
    )
    TestShard.master.respondWithUpdateResult(Seq(true, false, true))
    val result = deactivatorQueueDao.setProcessed(
      Seq(
        ExternalOfferCheckResult("hash1", ExternalOfferStatus.ACTIVE, "reason"),
        ExternalOfferCheckResult("hash2", ExternalOfferStatus.INACTIVE, "reason"),
        ExternalOfferCheckResult("hash3", ExternalOfferStatus.PARSE_ERROR, "reason"),
        ExternalOfferCheckResult("hash4", ExternalOfferStatus.RETRIABLE_ERROR, "reason"),
        ExternalOfferCheckResult("hash5", ExternalOfferStatus.PARSE_RETRIABLE_ERROR, "reason")
      )
    )
    assert(result("hash1"))
    assert(!result("hash2"))
    assert(result("hash3"))
    assert(!result("hash4"))
    assert(!result("hash5"))
    TestShard.master.verifyAllExpectedQueriesCalled()
  }
}
