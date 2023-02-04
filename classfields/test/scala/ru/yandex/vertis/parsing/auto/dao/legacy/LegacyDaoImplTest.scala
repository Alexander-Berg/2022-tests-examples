package ru.yandex.vertis.parsing.auto.dao.legacy

import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.dao.model.jooq.legacy.tables.{ParsingSales, ParsingSalesFilters, ParsingSalesSentPhones}
import ru.yandex.vertis.parsing.auto.util.TestDataUtils
import ru.yandex.vertis.parsing.util.dao.TestShard
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.util.{DateUtils, Md5Utils}
import ru.yandex.vertis.tracing.Traced

/**.hasTextval trace: Traced = Traced.empty
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class LegacyDaoImplTest extends FunSuite {
  private val legacyDao = new LegacyDaoImpl(TestShard)
  private val s = ParsingSales.PARSING_SALES
  private val f = ParsingSalesFilters.PARSING_SALES_FILTERS
  private val p = ParsingSalesSentPhones.PARSING_SALES_SENT_PHONES

  implicit private val trace: Traced = TracedUtils.empty

  test("getFilters") {
    val avitoUrl = TestDataUtils.testAvitoCarsUrl
    val avitoUrl2 = TestDataUtils.testAvitoCarsUrl
    val avitoUrl3 = TestDataUtils.testAvitoCarsUrl
    TestShard.slave.expectQuery(
      "select `all7`.`parsing_sales`.`url`, `all7`.`parsing_sales`.`status`, " +
        "group_concat(`all7`.`parsing_sales_filters`.`filter_name`) " +
        "from `all7`.`parsing_sales` left outer join `all7`.`parsing_sales_filters` " +
        "on `all7`.`parsing_sales_filters`.`parsing_sale_id` = `all7`.`parsing_sales`.`id` " +
        "where `all7`.`parsing_sales`.`hash` in (?, ?, ?) group by `all7`.`parsing_sales`.`hash`",
      Seq(Md5Utils.md5(avitoUrl), Md5Utils.md5(avitoUrl2), Md5Utils.md5(avitoUrl3))
    )
    TestShard.slave.respondWithQueryResult(s.URL, s.STATUS, DSL.field("g", classOf[String]))(
      Seq(avitoUrl, Byte.box(2), "filter1,filter2"),
      Seq(avitoUrl2, Byte.box(1), null)
    )
    val result = legacyDao.getFilters(Seq(avitoUrl, avitoUrl2, avitoUrl3))
    assert(
      result == Map(
        avitoUrl -> Some(LegacyData(2, Seq("filter1", "filter2"))),
        avitoUrl2 -> Some(LegacyData(1, Seq.empty)),
        avitoUrl3 -> None
      )
    )
  }

  test("getStatus") {
    val avitoUrl = TestDataUtils.testAvitoCarsUrl
    val avitoUrl2 = TestDataUtils.testAvitoCarsUrl
    TestShard.slave.expectQuery(
      "select `all7`.`parsing_sales`.`url`, `all7`.`parsing_sales`.`status` from `all7`.`parsing_sales` where " +
        "`all7`.`parsing_sales`.`hash` in (?, ?)",
      Seq(Md5Utils.md5(avitoUrl), Md5Utils.md5(avitoUrl2))
    )
    TestShard.slave.respondWithQueryResult(s.URL, s.STATUS)(
      Seq(avitoUrl, Byte.box(1)),
      Seq(avitoUrl2, Byte.box(2))
    )
    val result = legacyDao.getStatus(Seq(avitoUrl, avitoUrl2))
    assert(result == Map(avitoUrl -> Some(1), avitoUrl2 -> Some(2)))
  }

  test("getSentPhones") {
    val phone1 = "79991112233"
    val phone2 = "79991112244"
    TestShard.slave.expectQuery(
      "select `all7`.`parsing_sales_sent_phones`.`phone`, `all7`.`parsing_sales_sent_phones`.`create_date` " +
        "from `all7`.`parsing_sales_sent_phones` " +
        "where `all7`.`parsing_sales_sent_phones`.`phone` in (?, ?)",
      Seq(Long.box(phone1.toLong), Long.box(phone2.toLong))
    )
    val date = DateTime.now()
    TestShard.slave.respondWithSingleQueryResult(p.PHONE, p.CREATE_DATE)(
      Long.box(phone1.toLong),
      DateUtils.date2sqlTs(date)
    )
    val result = legacyDao.getSentPhones(Seq(phone1, phone2))
    assert(result.size == 2)
    assert(result(phone1).contains(date))
    assert(result(phone2).isEmpty)
  }

  test("setSent") {
    val url1 = TestDataUtils.testAvitoCarsUrl
    val url2 = TestDataUtils.testAvitoCarsUrl
    TestShard.master.expectBatchQuery(
      Seq(
        s"update `all7`.`parsing_sales` set `all7`.`parsing_sales`.`status` = 4 where `all7`.`parsing_sales`.`hash` = '${Md5Utils
          .md5(url1)}'",
        s"update `all7`.`parsing_sales` set `all7`.`parsing_sales`.`status` = 4 where `all7`.`parsing_sales`.`hash` = '${Md5Utils
          .md5(url2)}'"
      )
    )
    TestShard.master.respondWithUpdateResult(
      Seq(true, false)
    )
    val result = legacyDao.setSent(Seq(url1, url2))
    assert(result == Map(url1 -> true, url2 -> false))
  }

  test("countPrivateCreatedOffers") {
    val d1 = new DateTime(2018, 1, 1, 0, 0, 0, 0)
    val d2 = new DateTime(2019, 1, 1, 0, 0, 0, 0)
    TestShard.slave.expectQuery(
      "select count(*) from `all7`.`sales` where (`all7`.`sales`.`create_date` >= ? " +
        "and `all7`.`sales`.`create_date` < ? " +
        "and `all7`.`sales`.`new_client_id` is null)",
      Seq(DateUtils.date2sqlTs(d1), DateUtils.date2sqlTs(d2))
    )
    TestShard.slave.respondWithSingleQueryResult(DSL.field("count", classOf[Int]))(Int.box(15))
    val res = legacyDao.countCreatedPrivateOffers(d1, d2)
    assert(res == 15)
  }
}
