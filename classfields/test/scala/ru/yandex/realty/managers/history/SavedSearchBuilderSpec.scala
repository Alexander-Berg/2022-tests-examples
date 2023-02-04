package ru.yandex.realty.managers.history

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.realty.SpecBase
import ru.yandex.realty.saved.search.api.{Query, QueryParam}

/*@RunWith(classOf[JUnitRunner])
class SavedSearchBuilderSpec extends SpecBase {
  private val unicodeSearchId = "Айдишник"
  private val asciiSearchId = "Search id"
  private val paramName = "parameter"
  private val unicodeParamValue = "значение"
  private val asciiParamValue = "value"

  "SavedSearchBuilder in buildHttpQuery" should {
    "correctly encode http query if it contains unicode search id and unicode param value" in {
      val result = buildHttpQuery(unicodeSearchId, unicodeParamValue)
      val expectedResult = "parameter=%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5&" +
        "subscriptionDuplicationId=%D0%90%D0%B9%D0%B4%D0%B8%D1%88%D0%BD%D0%B8%D0%BA"
      result shouldEqual expectedResult
    }

    "correctly encode http query if it contains unicode search id and ascii param value" in {
      val result = buildHttpQuery(unicodeSearchId, asciiParamValue)
      val expectedResult = "parameter=value&subscriptionDuplicationId=%D0%90%D0%B9%D0%B4%D0%B8%D1%88%D0%BD%D0%B8%D0%BA"
      result shouldEqual expectedResult
    }

    "correctly encode http query if it contains ascii search id and unicode param value" in {
      val result = buildHttpQuery(asciiSearchId, unicodeParamValue)
      val expectedResult =
        "parameter=%D0%B7%D0%BD%D0%B0%D1%87%D0%B5%D0%BD%D0%B8%D0%B5&subscriptionDuplicationId=Search+id"
      result shouldEqual expectedResult
    }

    "correctly encode http query if it contains ascii search id and ascii param value" in {
      val result = buildHttpQuery(asciiSearchId, asciiParamValue)
      val expectedResult = "parameter=value&subscriptionDuplicationId=Search+id"
      result shouldEqual expectedResult
    }
  }

  private def buildHttpQuery(searchId: String, paramValue: String): String = {
    SavedSearchBuilder.buildHttpQuery(
      searchId,
      Query
        .newBuilder()
        .addParams(
          QueryParam
            .newBuilder()
            .setName(paramName)
            .addValues(paramValue)
            .build()
        )
        .build()
    )
  }
}*/
