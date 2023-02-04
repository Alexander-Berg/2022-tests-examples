package ru.auto.cabinet.reporting.pdf

import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import org.scalatest.matchers.should.Matchers
import ru.auto.cabinet.reporting.DataItem
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._

class HtmlComposerSpec extends FlatSpecLike with Matchers {

  import HtmlComposerSpec._

  behavior.of("HtmlComposer")

  it should "match paged json page by page" in {
    val result = HtmlComposer.asJson(items)
    val resultText = pretty(render(result))
    val expectedText = pretty(render(expectedJson))

    def matchPage(page: String): Unit = {
      (result \ Main \ page).diff(expectedJson \ Main \ page) match {
        case Diff(JNothing, JNothing, JNothing) =>
        case _ =>
          fail(
            s"$page page not matched json: $resultText expected json: $expectedText")
      }
    }
    List(FirstPage, SecondPage, ThirdPage).foreach(matchPage)
  }

}

object HtmlComposerSpec {
  val Main = "pages"
  val FirstPage = "first"
  val SecondPage = "second"
  val ThirdPage = "third"
  val Group1 = "group1"
  val Group2 = "group2"
  val Field1 = "field1"
  val Field2 = "field2"
  val Field3 = "field3"
  val Value1 = "some"
  val Value2 = 666.0
  val Value3 = 13.0
  val FirstPageData = List(Field1 -> Value1, Field2 -> Value2, Field3 -> Value3)

  val SecondPageData1: List[List[Any]] =
    List(List(Value1, Value2, Value3), List(Value1, Value3, Value2))
  val SecondPageData2 = List(Value1)
  val SecondPageData3 = List(Value2)
  val SecondPageData4: List[Any] = List(Value1, Value2, Value3)

  val items = List(
    List(TestDataItem(FirstPage, FirstPageData)),
    List(
      TestDataItem(s"$SecondPage.$Field1", SecondPageData1),
      TestDataItem(s"$SecondPage.$Group1.$Field2", SecondPageData2),
      TestDataItem(s"$SecondPage.$Group1.$Field3", SecondPageData3),
      TestDataItem(s"$SecondPage.$Field3", SecondPageData4)
    ),
    List(TestDataItem(s"$ThirdPage.$Field1", List()))
  )

  private val valueMapper: PartialFunction[Any, JValue] = {
    case n: Int => JInt(n)
    case n: Double => JDouble(n)
    case other => JString(other.toString)
  }

  private val expectedJson: JValue = Main -> Map(
    FirstPage -> FirstPageData.toMap.view.mapValues(valueMapper).toMap,
    SecondPage -> Map(
      Field1 -> JArray(
        SecondPageData1.map(sub => JArray(sub.map(valueMapper)))),
      Group1 -> JObject(
        Field2 -> valueMapper(Value1),
        Field3 -> valueMapper(Value2)),
      Field3 -> JArray(SecondPageData4.map(valueMapper))
    ),
    ThirdPage -> Map(Field1 -> JArray(Nil))
  )

}

private[pdf] case class TestDataItem(key: String, values: List[Any])
    extends DataItem
