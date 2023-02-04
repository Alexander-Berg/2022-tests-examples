package ru.yandex.vertis.hipe.clients.searcher

import org.junit.runner.RunWith
import org.scalatest.OptionValues
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import ru.yandex.vertis.hipe.clients.{HttpClientSpec, MockedHttpClient}

/**
  * Created by andrey on 10/2/17.
  */
@RunWith(classOf[JUnitRunner])
class DefaultSearcherClientTest extends HttpClientSpec with MockedHttpClient with PropertyChecks with OptionValues {
  private val searcherClient = new DefaultSearcherClient(http)

  "searcherClient" should {
    "return mark and model names" in {
      val mark = "AUDI"
      val model = "Q7"
      http.expect("GET", s"/catalogMarkModels?mark=$mark&model=$model")
      val json = """{
                   |   "data" : [
                   |      {
                   |         "parsed-query" : {
                   |            "filters" : {
                   |               "currency" : "RUR",
                   |               "rid_from_text" : false,
                   |               "locale_code" : "ru",
                   |               "credit" : false,
                   |               "mark" : [
                   |                  {
                   |                     "model" : [
                   |                        {
                   |                           "id" : "Q7",
                   |                           "generations-count" : 3,
                   |                           "name" : "Q7"
                   |                        }
                   |                     ],
                   |                     "id" : "AUDI",
                   |                     "name" : "Audi"
                   |                  }
                   |               ],
                   |               "buyout" : false
                   |            }
                   |         },
                   |         "pager" : {
                   |            "page-size" : 30,
                   |            "total-page-count" : 1,
                   |            "to" : 1,
                   |            "available-page-count" : 1,
                   |            "count" : 1,
                   |            "from" : 1,
                   |            "current" : 1
                   |         },
                   |         "marks" : [
                   |            {
                   |               "id" : "3139",
                   |               "code" : "AUDI",
                   |               "model" : [
                   |                  {
                   |                     "name" : "Q7",
                   |                     "car_ads_count" : 1441,
                   |                     "autoru-id" : "4428",
                   |                     "code" : "Q7",
                   |                     "id" : "3486"
                   |                  }
                   |               ],
                   |               "autoru-id" : "15",
                   |               "car_ads_count" : 15381,
                   |               "name" : "Audi"
                   |            }
                   |         ]
                   |      }
                   |   ],
                   |   "errors" : []
                   |}""".stripMargin
      http.respondWithJson(200, json)

      val result = searcherClient.getMarkModelNames(mark, model).futureValue
      result.markName.value shouldBe "Audi"
      result.modelName.value shouldBe "Q7"
    }
  }
}
