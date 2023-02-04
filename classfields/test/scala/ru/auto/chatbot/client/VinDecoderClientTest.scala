package ru.auto.chatbot.client

import org.scalatest.FunSuite
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client.{ApacheHttpClient, HttpClient, HttpEndpoint, LoggedHttpClient, MeteredHttpClient, MonitoredHttpClient, RemoteHttpService, RetryHttpClient, TracedHttpClient}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-03-26.
  */
class VinDecoderClientTest extends FunSuite {

  private val httpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val vinDecoderHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("vin-decoder-api-01-sas.test.vertis.yandex.net", 36314, "http")
    new RemoteHttpService(name = "vin_decoder", endpoint = endpoint, client = httpClient)
  }

  private val client = new VinDecoderClient(vinDecoderHttpService)

  ignore("get vin by lp") {
    val res = Await.result(client.getVinByLicensePlate("K718CE178"), 10.seconds)

    assert(res.getVin == "XTA21099043576182")
  }

  ignore("hypothesis result") {

    val res = Await.result(client.getHypothesis("Z94CC41BBER184593", "HYUNDAI", "SOLARIS"), 10.seconds)

    assert(res.result == "OK")
  }
}
