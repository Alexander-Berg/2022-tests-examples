package ru.yandex.vertis.parsing.realty.scheduler.workers.sender.letters

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.common.Site

@RunWith(classOf[JUnitRunner])
class LetterTest extends FunSuite {
  test("tricky user names") {
    val letter = Letter(
      Seq(
        LetterRow(
          Seq(Site.Avito),
          Seq("Руслан; Ирина"),
          "79058730501",
          1,
          Seq("Архангельская область"),
          Seq("Плесецк"),
          Seq("Россия, Архангельская область, рабочий посёлок Плесецк, Кооперативная улица, 30"),
          Seq("Продажа дома"),
          Seq(2500000L),
          Seq("https://www.avito.ru/plesetsk/doma_dachi_kottedzhi/dom_70_m_na_uchastke_12_sot._579222601"),
          Seq(18348L)
        )
      ),
      ids => "http://example.com/" + LetterRow.makeHash(ids)
    )

    assert(
      letter.csvRows.head == "avito;Руслан, Ирина;79058730501;1;Архангельская область;Плесецк;Россия, Архангельская область, рабочий посёлок Плесецк, Кооперативная улица, 30;Продажа дома;2500000;https://www.avito.ru/plesetsk/doma_dachi_kottedzhi/dom_70_m_na_uchastke_12_sot._579222601;http://example.com/E7u2b8k6u4EDdrgKz5t%2BYw%3D%3D"
    )
  }

  test("tricky user names 2") {
    val letter = Letter(
      Seq(
        LetterRow(
          Seq(Site.Avito),
          Seq("\""),
          "79058730501",
          1,
          Seq("Архангельская область"),
          Seq("Плесецк"),
          Seq("Россия, Архангельская область, рабочий посёлок Плесецк, Кооперативная улица, 30"),
          Seq("Продажа дома"),
          Seq(2500000L),
          Seq("https://www.avito.ru/plesetsk/doma_dachi_kottedzhi/dom_70_m_na_uchastke_12_sot._579222601"),
          Seq(18348L)
        )
      ),
      ids => "http://example.com/" + LetterRow.makeHash(ids)
    )

    assert(
      letter.csvRows.head == "avito;;79058730501;1;Архангельская область;Плесецк;Россия, Архангельская область, рабочий посёлок Плесецк, Кооперативная улица, 30;Продажа дома;2500000;https://www.avito.ru/plesetsk/doma_dachi_kottedzhi/dom_70_m_na_uchastke_12_sot._579222601;http://example.com/E7u2b8k6u4EDdrgKz5t%2BYw%3D%3D"
    )
  }
}
