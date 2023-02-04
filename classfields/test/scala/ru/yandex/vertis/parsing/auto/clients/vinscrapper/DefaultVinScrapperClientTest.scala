package ru.yandex.vertis.parsing.auto.clients.vinscrapper

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.parsing.clients.MockedHttpClientSupport

/**
  * TODO
  *
  * @author aborunov
  */
@RunWith(classOf[JUnitRunner])
class DefaultVinScrapperClientTest extends FunSuite with MockedHttpClientSupport {
  private val vinScrapperClient = new DefaultVinScrapperClient(http)

  test("scrape") {
    http.expect(
      "GET",
      "/scrape?url=https%3A%2F%2Fwww.avito.ru%2Fsochi%2Favtomobili%2Fhyundai_grand_starex_2008_1303252701"
    )
    http.respondWithJson("""{"status":"match", "vin":["VIN1"]}""")
    val result =
      vinScrapperClient.scrape("https://www.avito.ru/sochi/avtomobili/hyundai_grand_starex_2008_1303252701").futureValue
    assert(result.status == "match")
    assert(result.vin.nonEmpty)
    assert(result.vin.get.nonEmpty)
    assert(result.vin.get.head == "VIN1")
  }
}
