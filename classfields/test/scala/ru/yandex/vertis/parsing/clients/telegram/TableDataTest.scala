package ru.yandex.vertis.parsing.clients.telegram

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.util.table.TableData

/**
  * Test for TableData class
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class TableDataTest extends FunSuite {
  test("headers and rows length") {
    intercept[RuntimeException] {
      TableData("Test Table", Seq("head1", "h2", "header3"), Seq(Seq("e1", "elem2")))
    }
  }

  test("toHtml") {
    val tableData = TableData(
      "Test Table",
      Seq("head1", "h2", "header3"),
      Seq(
        Seq("e1", "elem2", "this is elem 3"),
        Seq("here is elem 4", "e5", "elem 6")
      )
    )
    assert(tableData.toHtml == """Test Table:
                            /<pre>|          head1 |    h2 |        header3 |
                                 /|             e1 | elem2 | this is elem 3 |
                                 /| here is elem 4 |    e5 |         elem 6 |
                                 /</pre>""".stripMargin('/'))
  }
}
