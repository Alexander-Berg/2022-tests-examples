package ru.yandex.auto.vin.decoder.money

import org.scalatest.funsuite.AnyFunSuite
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class YandexMoneyClientIntTest extends AnyFunSuite {

  val commonHttpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val yandexMoneyHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("money.yandex.ru", 443, "https")
    new RemoteHttpService(name = "yandex_money", endpoint = endpoint, client = commonHttpClient)
  }

  private val token = "" // https://yav.yandex-team.ru/secret/sec-01e6y87d5xr0peqetcjm8a51sj/explore/versions

  private val client = new YandexMoneyClient(yandexMoneyHttpService, token)

  ignore("get fines") {
    val sts = "9920088333"
    /*9920088333
      50УУ427578
      50ХМ916004
      7750840061*/
    val request = CreateRequest(vehicleCertificates = Seq(sts), driverLicenses = Seq.empty)
    val resF = for {
      requestId <- client.createRequest(request)
      _ = Thread.sleep(requestId.result.suggestedTimeout)
      response <- client.getFines(requestId.result.requestId)
    } yield response

    val res = Await.result(resF, 10.seconds)
    res
  }
}
