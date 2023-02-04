package ru.yandex.vertis.parsing.realty.scheduler.workers.sender.letters.bizdev

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.realty.scheduler.workers.sender.letters.LetterRow

@RunWith(classOf[JUnitRunner])
class BizdevLetterTest extends FunSuite {
  test("letter test") {
    val letter = BizdevLetter(
      Seq(
        BizdevLetterRow(
          "79058730501",
          Seq("Руслан; Ирина"),
          1,
          Map("Продажа дома" -> 1),
          Seq(18348L),
          "userId",
          ""
        )
      ),
      ids => "http://example.com/" + LetterRow.makeHash(ids)
    )

    assert(
      letter.csvRows.head == "79058730501;Руслан, Ирина;1;Продажа дома: 1;http://example.com/E7u2b8k6u4EDdrgKz5t%2BYw%3D%3D;userId"
    )
  }

  test("merge test") {
    val row1 = BizdevLetterRow(
      "79058730501",
      Seq("Руслан; Ирина"),
      1,
      Map("Продажа дома" -> 1),
      Seq(1L),
      "",
      "http://test.test"
    )
    val row2 = BizdevLetterRow(
      "79058730501",
      Seq("Руслан; Ирина"),
      1,
      Map("Продажа дома" -> 1),
      Seq(2L),
      "",
      ""
    )
    val row3 = BizdevLetterRow(
      "79058730501",
      Seq("Руслан; Ирина"),
      1,
      Map("Продажа квартиры" -> 1),
      Seq(3L),
      "",
      ""
    )
    val letter = BizdevLetter(Seq(row1.merge(row2).merge(row3)), ids => "http://example.com/" + LetterRow.makeHash(ids))
    assert(
      letter.csvRows.head == "79058730501;Руслан, Ирина;3;\"Продажа дома: 2,Продажа квартиры: 1\";http://example.com/qvj3%2BYISW9cUrYQwCgHu2A%3D%3D;http://test.test"
    )
  }

  test("merge test 2") {
    val row1 = BizdevLetterRow(
      "79058730501",
      Seq("Руслан; Ирина"),
      1,
      Map("Продажа дома" -> 1),
      Seq(1L),
      "",
      ""
    )
    val row2 = BizdevLetterRow(
      "79058730502",
      Seq("Руслан; Ирина"),
      1,
      Map("Продажа дома" -> 1),
      Seq(2L),
      "",
      ""
    )
    intercept[IllegalArgumentException](row1.merge(row2))
  }
}
