package ru.yandex.vertis.moderation.httpclient.wizard

import org.asynchttpclient.{DefaultAsyncHttpClient, DefaultAsyncHttpClientConfig}
import org.junit.Ignore
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ru.yandex.vertis.moderation.SpecBase
import ru.yandex.vertis.moderation.httpclient.wizard.impl.HttpWizardClient

import scala.concurrent.ExecutionContext

@RunWith(classOf[JUnitRunner])
@Ignore("For manually run")
class HttpWizardClientSpec extends SpecBase {

  import ExecutionContext.Implicits.global
  private val httpClient = new DefaultAsyncHttpClient()

  lazy val client = new HttpWizardClient("http://hamzard.yandex.net:8891", httpClient)

  "HttpWizardClient" should {
    "resolveBestGeoIds" in {
      // checks that -1 is not in result set
      client
        .resolveBestGeoIds(
          "Продажа однокомнатной квартиры на улице переулок Льва Толстого, 14" +
            " в Курске - МИР КВАРТИРкурск;переулок льва толстого;14"
        )
        .futureValue shouldBe Set(8)
      // checks that method returns empty set for text without address
      client
        .resolveBestGeoIds("Аренда двухкомнатной квартиры Бородина, 00 - объявление 44167892")
        .futureValue shouldBe Set()
      // checks that method works fine for not unique address
      client
        .resolveBestGeoIds(
          "Продажа однокомнатной квартиры 51 м² на Змеиногорском тракте, д. 1 " +
            "(г. Барнаул), цена 1 200 000 руб. - объявление № 17068 РосНДВгород барнаул;змеиногорский тракт;д 1"
        )
        .futureValue shouldBe Set(197)
      // checks that client works fine for text with # symbols
      client
        .resolveBestGeoIds(
          "Продажа однокомнатной квартиры 51 м² на Змеиногорском тракте, д. 1 " +
            "(г. Барнаул), цена 1 200 000 руб. - объявление #17068"
        )
        .futureValue shouldBe Set(197)
    }
  }
}
