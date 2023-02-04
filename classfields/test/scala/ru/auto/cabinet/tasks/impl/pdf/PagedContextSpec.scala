package ru.auto.cabinet.tasks.impl.pdf

import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.{AnyFlatSpecLike => FlatSpecLike}
import ru.auto.cabinet.reporting.DataItem
import ru.auto.cabinet.environment._

import scala.util.{Failure, Success}

class PagedContextSpec extends FlatSpecLike with Matchers with PagesSpec {
  behavior.of("PagedContext")

  import TestData._

  private val subj = PagedContext("someTitle", Map(), AlwaysOkPagedDataChecker)

  it should "sum dealer offer data into multi value item" in {
    val sourceData = DealerOfferCountItems
    val expectedData = asAnnotatedPairs(sourceData)
    val result =
      subj.tryCombine("sale", sourceData, TestReportDate.asJavaLocalDate())
    val (multies, others) = result.partition(_.isInstanceOf[MultiValueItem])
    others should be(empty)
    val items = multies.map(_.asInstanceOf[MultiValueItem])
    items.size should be(1)
    items.head.key should be("sale.cars_used.charts.sales")
    (items.head.values should contain)
      .theSameElementsInOrderAs(expectedData("sale.cars_used.charts.sales"))
  }

  /*
  it should "sum views and calls data into two multi value items" in {
    val sourceData = ViewWithTeleponyCallsItems
    val expectedData = asAnnotatedSingles(sourceData)
    val expectedKeys = List("call.charts.views", "call.charts.calls")
    val result = subj.tryCombine("calls", sourceData, TestReportDate)
    val (multies, others) = result.partition(_.isInstanceOf[MultiValueItem])
    others should be(empty)
    val items = multies.map(_.asInstanceOf[MultiValueItem])
    items.map(_.key) should contain theSameElementsAs expectedKeys
    for (key <- expectedKeys) {
      items.find(_.key == key).get.values should contain theSameElementsInOrderAs expectedData(key)
    }
  }

   */
  it should "avg offer placement durations into multi value item" in {
    val sourceData =
      (DealerOfferDurationsItems ++ TotalDealerOfferDurationsItems)
        .filter(_.key.key != "period.numSales")
    val expectedData = asPairs(sourceData)
    val result =
      subj.tryCombine("period", sourceData, TestReportDate.asJavaLocalDate())
    val (multies, others) = result.partition(_.isInstanceOf[MultiValueItem])
    others should be(empty)
    val items = multies.map(_.asInstanceOf[MultiValueItem])
    items.size should be(1)
    items.head.key should be("period.charts.days")
    (items.head.values should contain)
      .theSameElementsInOrderAs(expectedData.values.head)
  }

  it should "sum offer counts into multi value item" in {
    val sourceData =
      (DealerOfferDurationsItems ++ TotalDealerOfferDurationsItems)
        .filter(_.key.key != "period.charts.days")
    val expectedData = asTuples(sourceData)
    val result =
      subj.tryCombine("period", sourceData, TestReportDate.asJavaLocalDate())
    val (multies, others) = result.partition(_.isInstanceOf[MultiValueItem])
    others should be(empty)
    val items = multies.map(_.asInstanceOf[MultiValueItem])
    items.size should be(1)
    items.head.key should be("period.numSales")
    items.head.values should contain theSameElementsAs expectedData.values.head
  }

  it should "select latest revoked count" in {
    val sourceData = RevokedOfferItems
    val result =
      subj.tryCombine("period", sourceData, TestReportDate.asJavaLocalDate())
    val (multies, others) = result.partition(_.isInstanceOf[MultiValueItem])
    others should be(empty)
    val items = multies.map(_.asInstanceOf[MultiValueItem])
    items.size should be(1)
    items.head.key should be("period.steps")
    items.head.values should contain theSameElementsAs List(32.0)
  }

  it should "fail on inconsistent chronological data" in {
    val subj = TestDataChecker(10)
    PagedContext
      .groupSeries(30, ViewWithTeleponyCallsItems)(subj) should matchPattern {
      case Failure(NotEnoughDataException(_)) =>
    }
  }

  it should "succeed on consistent chronological data" in {
    val subj = TestDataChecker(29)
    PagedContext
      .groupSeries(30, ViewWithTeleponyCallsItems)(subj) should matchPattern {
      case Success(items) if items.asInstanceOf[List[DataItem]].size > 1 =>
    }
  }

  private case class TestDataChecker(threshold: Int) extends PagedDataChecker {

    def checkConsistency(
        dataKey: Any,
        expectedLength: Int,
        actualLength: Int): Unit =
      dataKey match {
        case PagesParts.CallsCharts
            if expectedLength - actualLength > threshold =>
          throw NotEnoughDataException("Exception")
        case _ =>
      }

    def checkKeySet(page: String, keys: Set[Any]): Unit = ()
  }

}
