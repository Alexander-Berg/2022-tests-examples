package ru.yandex.vertis.parsing.auto.dao.holocron

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.vertis.holocron.common.{Classified, HoloOffer}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.THolocronSendData
import ru.yandex.vertis.parsing.components.time.TimeService
import ru.yandex.vertis.parsing.holocron.HolocronSendResult
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.dao.TestShard
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.tracing.Traced

import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class HolocronDaoImplSecondTest extends FunSuite with MockitoSupport {
  private val mockedTimeService = mock[TimeService]

  private val holocronDao = new HolocronDaoImpl(TestShard, mockedTimeService)

  private val t = THolocronSendData.T_HOLOCRON_SEND_DATA

  implicit private val trace: Traced = TracedUtils.empty

  test("updateResults: empty") {
    holocronDao.updateResults(Seq.empty, 5.minutes)
  }

  test("updateResults: nonEmpty") {
    when(mockedTimeService.getNow).thenReturn(new DateTime(2019, 2, 16, 0, 0, 0, 0))
    val results = Seq(
      HolocronSendResult.SendAttempt(1, HoloOffer.getDefaultInstance, Success(())),
      HolocronSendResult.SendAttempt(2, HoloOffer.getDefaultInstance, Failure(new RuntimeException("Error!"))),
      HolocronSendResult.UnableToConvert(3, Classified.AVITO),
      HolocronSendResult.UnableToValidate(4, Classified.AVITO)
    )
    TestShard.master.expectBatchQuery(
      Seq(
        "update `parsing`.`t_holocron_send_data` set " +
          "`parsing`.`t_holocron_send_data`.`error_message` = null, " +
          "`parsing`.`t_holocron_send_data`.`update_date` = {ts '2019-02-16 00:00:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`work_date` = null, " +
          "`parsing`.`t_holocron_send_data`.`made_attempts` = (`parsing`.`t_holocron_send_data`.`made_attempts` + 1), " +
          "`parsing`.`t_holocron_send_data`.`send_date` = {ts '2019-02-16 00:00:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`unable_to_convert` = 0 " +
          "where `parsing`.`t_holocron_send_data`.`id` = 1",
        "update `parsing`.`t_holocron_send_data` set " +
          "`parsing`.`t_holocron_send_data`.`error_message` = 'Error!', " +
          "`parsing`.`t_holocron_send_data`.`update_date` = {ts '2019-02-16 00:00:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`work_date` = {ts '2019-02-16 00:05:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`made_attempts` = (`parsing`.`t_holocron_send_data`.`made_attempts` + 1), " +
          "`parsing`.`t_holocron_send_data`.`unable_to_convert` = 0 " +
          "where `parsing`.`t_holocron_send_data`.`id` = 2",
        "update `parsing`.`t_holocron_send_data` set " +
          "`parsing`.`t_holocron_send_data`.`error_message` = null, " +
          "`parsing`.`t_holocron_send_data`.`update_date` = {ts '2019-02-16 00:00:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`work_date` = null, " +
          "`parsing`.`t_holocron_send_data`.`made_attempts` = (`parsing`.`t_holocron_send_data`.`made_attempts` + 1), " +
          "`parsing`.`t_holocron_send_data`.`unable_to_convert` = 1 " +
          "where `parsing`.`t_holocron_send_data`.`id` = 3",
        "update `parsing`.`t_holocron_send_data` set " +
          "`parsing`.`t_holocron_send_data`.`error_message` = null, " +
          "`parsing`.`t_holocron_send_data`.`update_date` = {ts '2019-02-16 00:00:00.0'}, " +
          "`parsing`.`t_holocron_send_data`.`work_date` = null, " +
          "`parsing`.`t_holocron_send_data`.`made_attempts` = (`parsing`.`t_holocron_send_data`.`made_attempts` + 1), " +
          "`parsing`.`t_holocron_send_data`.`unable_to_convert` = 2 " +
          "where `parsing`.`t_holocron_send_data`.`id` = 4"
      )
    )
    holocronDao.updateResults(results, 5.minutes)
  }

  test("getOldProcessedIds") {
    val older = new DateTime(2019, 11, 14, 0, 0, 0, 0)
    TestShard.master.expectQuery(
      "select `parsing`.`t_holocron_send_data`.`id` " +
        "from `parsing`.`t_holocron_send_data` " +
        "where (`parsing`.`t_holocron_send_data`.`work_date` is null " +
        "and `parsing`.`t_holocron_send_data`.`create_date` < ?) " +
        "limit ?",
      Seq(DateUtils.date2sqlTs(older), Int.box(100))
    )
    TestShard.master.respondWithEmptyQueryResult(t.ID)
    holocronDao.getOldProcessedIds(100, older)
    TestShard.master.verifyAllExpectedQueriesCalled()
  }
}
