package ru.yandex.vertis.parsing.auto.dao.photo

import org.joda.time.DateTime
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.enums.TParsedPhotosMdsNamespace
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.TParsedPhotos
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
class PhotoDaoImplTest extends FunSuite with MockitoSupport {
  private val p: TParsedPhotos = TParsedPhotos.T_PARSED_PHOTOS

  private val mockedTimeService = mock[TimeService]

  private val photoDao = new PhotoDaoImpl(TestShard, mockedTimeService)

  implicit private val trace: Traced = TracedUtils.empty

  test("updateStatus: success") {
    val date = new DateTime(2019, 2, 1, 10, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    TestShard.master.expectQuery(
      "update `parsing`.`t_parsed_photos` " +
        "set `parsing`.`t_parsed_photos`.`mds_name` = '1574795-328cb9e8eb8e97d8c3982be621e130f9', " +
        "`parsing`.`t_parsed_photos`.`error_message` = null, " +
        "`parsing`.`t_parsed_photos`.`update_date` = {ts '2019-02-01 10:00:00.0'}, " +
        "`parsing`.`t_parsed_photos`.`work_date` = null, " +
        "`parsing`.`t_parsed_photos`.`finish_date` = {ts '2019-02-01 10:00:00.0'}, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts` = (`parsing`.`t_parsed_photos`.`remain_attempts` - 1) " +
        "where `parsing`.`t_parsed_photos`.`id` = 1"
    )
    photoDao.updateStatus(
      Seq(
        PhotoRow(
          1L,
          Category.CARS,
          "https://82.img.avito.st/1280x960/5211332182.jpg",
          TParsedPhotosMdsNamespace.autoru_all,
          PhotoProcessResult.Success("1574795-328cb9e8eb8e97d8c3982be621e130f9")
        )
      )
    )
  }

  test("updateStatus: error") {
    val date = new DateTime(2019, 2, 1, 10, 0, 0, 0)
    when(mockedTimeService.getNow).thenReturn(date)
    TestShard.master.expectQuery(
      "update `parsing`.`t_parsed_photos` " +
        "set `parsing`.`t_parsed_photos`.`mds_name` = null, " +
        "`parsing`.`t_parsed_photos`.`error_message` = 'Failed request to downloader/download_from_url', " +
        "`parsing`.`t_parsed_photos`.`update_date` = {ts '2019-02-01 10:00:00.0'}, " +
        "`parsing`.`t_parsed_photos`.`work_date` = " +
        "case when `parsing`.`t_parsed_photos`.`remain_attempts` <= 1 then null " +
        "else {ts '2019-02-01 10:00:00.0'} end, " +
        "`parsing`.`t_parsed_photos`.`finish_date` = " +
        "case when `parsing`.`t_parsed_photos`.`remain_attempts` <= 1 then {ts '2019-02-01 10:00:00.0'} " +
        "else null end, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts` = (`parsing`.`t_parsed_photos`.`remain_attempts` - 1) " +
        "where `parsing`.`t_parsed_photos`.`id` = 1"
    )
    photoDao.updateStatus(
      Seq(
        PhotoRow(
          1L,
          Category.CARS,
          "https://82.img.avito.st/1280x960/5211332182.jpg",
          TParsedPhotosMdsNamespace.autoru_all,
          PhotoProcessResult.Error("Failed request to downloader/download_from_url")
        )
      )
    )
  }

  test("getWorkBatch") {
    val date = new DateTime(2019, 2, 1, 10, 0, 0, 0)
    val ts = DateUtils.date2sqlTs(date)
    when(mockedTimeService.getNow).thenReturn(date)
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos`.`id`, " +
        "`parsing`.`t_parsed_photos`.`offer_id`, " +
        "`parsing`.`t_parsed_photos`.`category`, " +
        "`parsing`.`t_parsed_photos`.`photo_url`, " +
        "`parsing`.`t_parsed_photos`.`mds_namespace`, " +
        "`parsing`.`t_parsed_photos`.`mds_name`, " +
        "`parsing`.`t_parsed_photos`.`error_message`, " +
        "`parsing`.`t_parsed_photos`.`create_date`, " +
        "`parsing`.`t_parsed_photos`.`update_date`, " +
        "`parsing`.`t_parsed_photos`.`work_date`, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos`.`finish_date` " +
        "from `parsing`.`t_parsed_photos` " +
        "where ((`parsing`.`t_parsed_photos`.`work_date` is not null and `parsing`.`t_parsed_photos`.`work_date` < ?) " +
        "or `parsing`.`t_parsed_photos`.`finish_date` is null) limit ?",
      Seq(ts, Int.box(100))
    )
    TestShard.slave.respondWithEmptyTableResult(p)
    photoDao.getWorkBatch(100)
  }

  test("getByUrl: not finished") {
    val url = "https://82.img.avito.st/1280x960/5211332182.jpg"
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos`.`id`, " +
        "`parsing`.`t_parsed_photos`.`offer_id`, " +
        "`parsing`.`t_parsed_photos`.`category`, " +
        "`parsing`.`t_parsed_photos`.`photo_url`, " +
        "`parsing`.`t_parsed_photos`.`mds_namespace`, " +
        "`parsing`.`t_parsed_photos`.`mds_name`, " +
        "`parsing`.`t_parsed_photos`.`error_message`, " +
        "`parsing`.`t_parsed_photos`.`create_date`, " +
        "`parsing`.`t_parsed_photos`.`update_date`, " +
        "`parsing`.`t_parsed_photos`.`work_date`, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos`.`finish_date` " +
        "from `parsing`.`t_parsed_photos` where `parsing`.`t_parsed_photos`.`photo_url` = ? " +
        "order by `parsing`.`t_parsed_photos`.`id` desc",
      Seq(url)
    )
    val errorMessage = "Failed request to downloader/download_from_url"
    TestShard.slave.respondWithSingleTableResult(p)(
      Long.box(1),
      null,
      "CARS",
      url,
      "autoru_all",
      null,
      errorMessage,
      null,
      null,
      null,
      null,
      null
    )
    val rows = photoDao.getByUrl(url)
    assert(rows.head.result == PhotoProcessResult.NotFinished)
  }

  test("getByUrl: error") {
    val errorMessage = "Failed request to downloader/download_from_url"
    val date = new DateTime(2019, 2, 1, 10, 0, 0, 0)
    val ts = DateUtils.date2sqlTs(date)
    val url = "https://82.img.avito.st/1280x960/5211332182.jpg"
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos`.`id`, " +
        "`parsing`.`t_parsed_photos`.`offer_id`, " +
        "`parsing`.`t_parsed_photos`.`category`, " +
        "`parsing`.`t_parsed_photos`.`photo_url`, " +
        "`parsing`.`t_parsed_photos`.`mds_namespace`, " +
        "`parsing`.`t_parsed_photos`.`mds_name`, " +
        "`parsing`.`t_parsed_photos`.`error_message`, " +
        "`parsing`.`t_parsed_photos`.`create_date`, " +
        "`parsing`.`t_parsed_photos`.`update_date`, " +
        "`parsing`.`t_parsed_photos`.`work_date`, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos`.`finish_date` " +
        "from `parsing`.`t_parsed_photos` where `parsing`.`t_parsed_photos`.`photo_url` = ? " +
        "order by `parsing`.`t_parsed_photos`.`id` desc",
      Seq(url)
    )
    TestShard.slave.respondWithSingleTableResult(p)(
      Long.box(1),
      null,
      "CARS",
      url,
      "autoru_all",
      null,
      errorMessage,
      null,
      null,
      null,
      null,
      ts
    )
    val rows = photoDao.getByUrl(url)
    assert(rows.head.result == PhotoProcessResult.Error(errorMessage))
  }

  test("getByUrl: success") {
    val mdsName = "1574795-328cb9e8eb8e97d8c3982be621e130f9"
    val date = new DateTime(2019, 2, 1, 10, 0, 0, 0)
    val ts = DateUtils.date2sqlTs(date)
    val url = "https://82.img.avito.st/1280x960/5211332182.jpg"
    TestShard.slave.expectQuery(
      "select `parsing`.`t_parsed_photos`.`id`, " +
        "`parsing`.`t_parsed_photos`.`offer_id`, " +
        "`parsing`.`t_parsed_photos`.`category`, " +
        "`parsing`.`t_parsed_photos`.`photo_url`, " +
        "`parsing`.`t_parsed_photos`.`mds_namespace`, " +
        "`parsing`.`t_parsed_photos`.`mds_name`, " +
        "`parsing`.`t_parsed_photos`.`error_message`, " +
        "`parsing`.`t_parsed_photos`.`create_date`, " +
        "`parsing`.`t_parsed_photos`.`update_date`, " +
        "`parsing`.`t_parsed_photos`.`work_date`, " +
        "`parsing`.`t_parsed_photos`.`remain_attempts`, " +
        "`parsing`.`t_parsed_photos`.`finish_date` " +
        "from `parsing`.`t_parsed_photos` where `parsing`.`t_parsed_photos`.`photo_url` = ? " +
        "order by `parsing`.`t_parsed_photos`.`id` desc",
      Seq(url)
    )
    TestShard.slave.respondWithSingleTableResult(p)(
      Long.box(1),
      null,
      "CARS",
      url,
      "autoru_all",
      mdsName,
      null,
      null,
      null,
      null,
      null,
      ts
    )
    val rows = photoDao.getByUrl(url)
    assert(rows.head.result == PhotoProcessResult.Success(mdsName))
  }
}
