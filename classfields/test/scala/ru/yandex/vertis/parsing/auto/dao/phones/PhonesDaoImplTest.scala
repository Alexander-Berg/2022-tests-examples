package ru.yandex.vertis.parsing.auto.dao.phones

import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.auto.dao.model.jooq.parsing.tables.{TParsedOffers, TParsedOffersPhones}
import ru.yandex.vertis.parsing.util.DateUtils
import ru.yandex.vertis.parsing.util.dao.TestShard
import ru.yandex.vertis.parsing.util.http.tracing.TracedUtils
import ru.yandex.vertis.parsing.validators.FilterReason
import ru.yandex.vertis.tracing.Traced

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class PhonesDaoImplTest extends FunSuite {
  private val parsingShard = new TestShard
  private val usersShard = new TestShard
  private val phonesDao = new PhonesDaoImpl(parsingShard, usersShard)

  private val o = TParsedOffers.T_PARSED_OFFERS
  private val p = TParsedOffersPhones.T_PARSED_OFFERS_PHONES

  implicit private val trace: Traced = TracedUtils.empty

  test("checkDealerPhones") {
    val phone1 = "79291112233"
    val phone2 = "79294445566"
    parsingShard.slave.expectQuery(
      "select `parsing`.`t_parsed_offers_phones`.`v_value` " +
        "from `parsing`.`t_parsed_offers_phones` " +
        "join `parsing`.`t_parsed_offers_filtered_reasons` " +
        "on `parsing`.`t_parsed_offers_phones`.`hash` = `parsing`.`t_parsed_offers_filtered_reasons`.`hash` " +
        "where (`parsing`.`t_parsed_offers_filtered_reasons`.`v_value` = ? " +
        "and `parsing`.`t_parsed_offers_phones`.`v_value` in (?, ?)) " +
        "group by `parsing`.`t_parsed_offers_phones`.`v_value`",
      Seq(FilterReason.IsDealer, Long.box(phone1.toLong), Long.box(phone2.toLong))
    )
    parsingShard.slave.respondWithSingleQueryResult(DSL.field("phones", classOf[String]))(phone1)
    val res = phonesDao.checkDealerPhones(Seq(phone1, phone2))
    assert(res.size == 2)
    assert(res.contains(phone1))
    assert(res(phone1))
    assert(!res(phone2))
  }

  test("getPhonesLastUpdate: null sent_date") {
    val phone1 = "73919898362"
    val phone2 = "79291112233"
    val date = new DateTime(2019, 2, 26, 0, 0, 0, 0)
    val ts = DateUtils.date2sqlTs(date)
    parsingShard.slave.expectQuery(
      "select `parsing`.`t_parsed_offers_phones`.`v_value`, " +
        "max(`parsing`.`t_parsed_offers`.`sent_date`) from `parsing`.`t_parsed_offers` " +
        "join `parsing`.`t_parsed_offers_phones` " +
        "on `parsing`.`t_parsed_offers`.`hash` = `parsing`.`t_parsed_offers_phones`.`hash` " +
        "where (`parsing`.`t_parsed_offers`.`status` in (?, ?, ?, ?) " +
        "and `parsing`.`t_parsed_offers_phones`.`v_value` in (?, ?)) " +
        "group by `parsing`.`t_parsed_offers_phones`.`v_value`",
      Seq("SENT", "PUBLISHED", "NOT_PUBLISHED", "OPENED", Long.box(phone1.toLong), Long.box(phone2.toLong))
    )
    parsingShard.slave.respondWithQueryResult(p.V_VALUE, o.SENT_DATE)(
      Seq(Long.box(phone1.toLong), null),
      Seq(Long.box(phone2.toLong), ts)
    )
    val result = phonesDao.getPhonesLastSend(Seq(phone1, phone2))
    assert(!result.contains(phone1))
    assert(result(phone2) == date)
  }
}
