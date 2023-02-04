package ru.yandex.vertis.parsing.auto.dao.meta

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.TParsedPhotosMetaInWork
import ru.yandex.vertis.parsing.auto.dao.parsedoffers.ParsedOffersDao
import ru.yandex.vertis.parsing.common.Site
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
class MetaDaoImplSecondTest extends FunSuite with MockitoSupport {
  private val mockedParsedOffersDao = mock[ParsedOffersDao]

  private val mockedTimeService = mock[TimeService]
  private val now = new DateTime(2019, 6, 21, 16, 0, 0, 0)
  when(mockedTimeService.getNow).thenReturn(now)

  private val metaDao = new MetaDaoImpl(TestShard, mockedParsedOffersDao, mockedTimeService)

  implicit private val trace: Traced = TracedUtils.empty

  test("getFinishedBatch") {
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos_meta_in_work`.`id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`photo_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`mds_name`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`meta`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`finished`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`error_message`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`create_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`update_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`work_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`send_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`deleted` " +
        "from `parsing`.`t_parsed_photos_meta_in_work` " +
        "where (" +
        "(`parsing`.`t_parsed_photos_meta_in_work`.`meta` is not null " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`send_date` is not null) " +
        "or " +
        "(`parsing`.`t_parsed_photos_meta_in_work`.`meta` is null and " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts` <= ?)) " +
        "limit ?",
      Seq(Int.box(0), Int.box(10))
    )
    TestShard.slave.respondWithEmptyTableResult(TParsedPhotosMetaInWork.T_PARSED_PHOTOS_META_IN_WORK)
    metaDao.getFinishedBatch(10)
    TestShard.slave.verifyAllExpectedQueriesCalled()
  }

  test("getNewWorkBatch") {
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos_meta_in_work`.`id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`photo_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`mds_name`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`meta`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`finished`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`error_message`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`create_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`update_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`work_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`send_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`deleted` " +
        "from `parsing`.`t_parsed_photos_meta_in_work` " +
        "join `parsing`.`t_parsed_offers` " +
        "on `parsing`.`t_parsed_offers`.`hash` = md5(`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id`) " +
        "where (`parsing`.`t_parsed_photos_meta_in_work`.`work_date` is not null " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`work_date` <= ? " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts` > ? " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id` like ? " +
        "and `parsing`.`t_parsed_offers`.`source` <> ?) limit ?",
      Seq(DateUtils.date2sqlTs(now), Int.box(0), "avito%", "PARSING_SALES", Int.box(100))
    )
    TestShard.slave.respondWithEmptyTableResult(TParsedPhotosMetaInWork.T_PARSED_PHOTOS_META_IN_WORK)
    metaDao.getNewWorkBatch(Site.Avito, 100)
    TestShard.slave.verifyAllExpectedQueriesCalled()
  }

  test("getOldWorkBatch") {
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos_meta_in_work`.`id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`photo_url`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`mds_name`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`meta`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`finished`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`error_message`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`create_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`update_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`work_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`send_date`, " +
        "`parsing`.`t_parsed_photos_meta_in_work`.`deleted` " +
        "from `parsing`.`t_parsed_photos_meta_in_work` " +
        "join `parsing`.`t_parsed_offers` " +
        "on `parsing`.`t_parsed_offers`.`hash` = md5(`parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id`) " +
        "where (`parsing`.`t_parsed_photos_meta_in_work`.`work_date` is not null " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`work_date` <= ? " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`remain_attempts` > ? " +
        "and `parsing`.`t_parsed_photos_meta_in_work`.`external_offer_id` like ? " +
        "and `parsing`.`t_parsed_offers`.`source` = ?) limit ?",
      Seq(DateUtils.date2sqlTs(now), Int.box(0), "avito%", "PARSING_SALES", Int.box(100))
    )
    TestShard.slave.respondWithEmptyTableResult(TParsedPhotosMetaInWork.T_PARSED_PHOTOS_META_IN_WORK)
    metaDao.getOldWorkBatch(Site.Avito, 100)
    TestShard.slave.verifyAllExpectedQueriesCalled()
  }
}
