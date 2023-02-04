package ru.yandex.vertis.parsing.auto.dao.vinscrapper

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.enums.TVinScrapperQueueStatus
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.{TParsedOffers, TVinScrapperQueue}
import ru.yandex.vertis.parsing.components.time.TimeService
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
class VinScrapperDaoImplSecondTest extends FunSuite with MockitoSupport {
  implicit private val trace: Traced = TracedUtils.empty

  private val mockedTimeService = mock[TimeService]

  when(mockedTimeService.getNow).thenReturn(new DateTime(2019, 5, 15, 0, 0, 0, 0))

  private val v = TVinScrapperQueue.T_VIN_SCRAPPER_QUEUE
  private val o = TParsedOffers.T_PARSED_OFFERS

  private val vinScrapperDao = new VinScrapperDaoImpl(TestShard, mockedTimeService)

  test("updateStatus") {
    TestShard.master.expectBatchQuery(
      Seq(
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'NEW', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = {ts '2019-05-15 00:00:00.0'} " +
          "where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash1'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'IN_PROGRESS', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`in_progress_date` = " +
          "ifnull(`parsing`.`t_vin_scrapper_queue`.`in_progress_date`, {ts '2019-05-15 00:00:00.0'}), " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = {ts '2019-05-15 00:00:00.0'} " +
          "where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash2'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'RETRY', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = {ts '2019-05-15 00:00:00.0'} " +
          "where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash3'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'FAILURE', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = null where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash4'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'MANY', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = null where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash5'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'MATCH', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = null where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash6'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'NO_RESULT', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = null where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash7'",
        "update `parsing`.`t_vin_scrapper_queue` set `parsing`.`t_vin_scrapper_queue`.`status` = 'UNEXPECTED', " +
          "`parsing`.`t_vin_scrapper_queue`.`update_date` = {ts '2019-05-15 00:00:00.0'}, " +
          "`parsing`.`t_vin_scrapper_queue`.`work_date` = null where `parsing`.`t_vin_scrapper_queue`.`hash` = 'hash8'"
      )
    )
    vinScrapperDao.updateStatus(
      Seq(
        ("hash1", TVinScrapperQueueStatus.NEW),
        ("hash2", TVinScrapperQueueStatus.IN_PROGRESS),
        ("hash3", TVinScrapperQueueStatus.RETRY),
        ("hash4", TVinScrapperQueueStatus.FAILURE),
        ("hash5", TVinScrapperQueueStatus.MANY),
        ("hash6", TVinScrapperQueueStatus.MATCH),
        ("hash7", TVinScrapperQueueStatus.NO_RESULT),
        ("hash8", TVinScrapperQueueStatus.UNEXPECTED)
      )
    )
    TestShard.master.verifyAllExpectedQueriesCalled()
  }

  test("getInProgress") {
    val time = new DateTime(2019, 5, 15, 0, 0, 0, 0)
    val ts = DateUtils.date2sqlTs(time)
    TestShard.slave.expectQuery(
      "select `parsing`.`t_vin_scrapper_queue`.`hash`, `parsing`.`t_parsed_offers`.`url`, " +
        "`parsing`.`t_vin_scrapper_queue`.`create_date`, `parsing`.`t_vin_scrapper_queue`.`in_progress_date`, " +
        "`parsing`.`t_vin_scrapper_queue`.`update_date` " +
        "from `parsing`.`t_vin_scrapper_queue` join `parsing`.`t_parsed_offers` " +
        "on `parsing`.`t_vin_scrapper_queue`.`hash` = `parsing`.`t_parsed_offers`.`hash` " +
        "where `parsing`.`t_vin_scrapper_queue`.`status` in (?, ?, ?) " +
        "order by case `parsing`.`t_vin_scrapper_queue`.`status` when ? then 0 when ? then 1 when ? then 2 end asc, " +
        "`parsing`.`t_vin_scrapper_queue`.`id` desc limit ?",
      Seq("IN_PROGRESS", "RETRY", "NEW", "IN_PROGRESS", "RETRY", "NEW", Int.box(10))
    )
    TestShard.slave.respondWithQueryResult(v.HASH, o.URL, v.CREATE_DATE, v.IN_PROGRESS_DATE, v.UPDATE_DATE)(
      Seq("hash1", "url1", ts, ts, ts),
      Seq("hash2", "url2", ts, null, ts)
    )
    val result = vinScrapperDao.getInProgress(10)
    assert(
      result == Seq(
        VinScrapperRow("hash1", "url1", time, Some(time), time),
        VinScrapperRow("hash2", "url2", time, None, time)
      )
    )
    TestShard.slave.verifyAllExpectedQueriesCalled()
  }
}
