package ru.yandex.vos2.autoru.services.searcher

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{Inspectors, OptionValues}
import ru.yandex.vertis.ops.OperationalSupport
import ru.yandex.vertis.ops.test.TestOperationalSupport
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.util.HttpBlockingPool.Instance
import ru.yandex.vos2.util.http.MockHttpClientHelper

/**
  * Created by andrey on 8/29/16.
  */
@RunWith(classOf[JUnitRunner])
class SearcherClientTest
  extends AnyFunSuite
  with MockHttpClientHelper
  with OptionValues
  with InitTestDbs
  with Inspectors {
  private val offer = getOfferById(1043045004)

  test("testUrl") {
    val searcherClient = new DefaultSearcherClient("http://dev33i.vs.os.yandex.net", 34389, rateLimit = 200) {
      override protected val client = new Instance(mockHttpClient(200, ""))

      override protected def operationalSupport: Option[OperationalSupport] = Some(TestOperationalSupport)

      override protected def prometheusClass: Class[_] = classOf[DefaultSearcherClient]
    }
    assert(searcherClient.createUrl(offer) == "http://dev33i.vs.os.yandex.net:34389/carAdById?id=autoru-1043045004")
  }

  test("test success parse json") {
    val expectedPos = 123
    val responseBody = s"""{"data": [{"position":$expectedPos}], "errors": []}"""
    val searcherClient = new DefaultSearcherClient("http://back-rt-01-sas.test.vertis.yandex.net", 34389, 200) {
      override protected val client = new Instance(mockHttpClient(200, responseBody))

      override protected def operationalSupport: Option[OperationalSupport] = Some(TestOperationalSupport)

      override protected def prometheusClass: Class[_] = classOf[DefaultSearcherClient]
    }
    val pos = searcherClient.getPosition(offer)
    assert(pos.contains(expectedPos))
  }

  test("test response 404 from get position") {
    val responseBody = s"""{"data": [{"position":-1}], "errors": []}"""
    val searcherClient = new DefaultSearcherClient("http://back-rt-01-sas.test.vertis.yandex.net", 34389, 200) {
      override protected val client = new Instance(mockHttpClient(404, responseBody))

      override protected def operationalSupport: Option[OperationalSupport] = Some(TestOperationalSupport)

      override protected def prometheusClass: Class[_] = classOf[DefaultSearcherClient]
    }
    val pos = searcherClient.getPosition(offer)
    assert(pos.isEmpty)
  }

  test("test response without data") {
    val responseBody = s"""{"errors": []}"""
    val searcherClient = new DefaultSearcherClient("http://back-rt-01-sas.test.vertis.yandex.net", 34389, 200) {
      override protected val client = new Instance(mockHttpClient(200, responseBody))

      override protected def operationalSupport: Option[OperationalSupport] = Some(TestOperationalSupport)

      override protected def prometheusClass: Class[_] = classOf[DefaultSearcherClient]
    }

    val pos = searcherClient.getPosition(offer)
    assert(pos.isEmpty)
  }

  test("test bad response") {
    val responseBody = s"""+-"""
    val searcherClient = new DefaultSearcherClient("http://back-rt-01-sas.test.vertis.yandex.net", 34389, 200) {
      override protected val client = new Instance(mockHttpClient(200, responseBody))

      override protected def operationalSupport: Option[OperationalSupport] = Some(TestOperationalSupport)

      override protected def prometheusClass: Class[_] = classOf[DefaultSearcherClient]
    }

    val pos = searcherClient.getPosition(offer)
    assert(pos.isEmpty)
  }
}
