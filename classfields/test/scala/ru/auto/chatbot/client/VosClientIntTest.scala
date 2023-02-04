package ru.auto.chatbot.client

import org.scalatest.FunSuite
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client.{ApacheHttpClient, HttpClient, HttpEndpoint, LoggedHttpClient, MonitoredHttpClient, RemoteHttpService, RetryHttpClient, TracedHttpClient}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-25.
  */
class VosClientIntTest extends FunSuite {

  private val httpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val vosHttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("vos2-autoru-api.vrts-slb.test.vertis.yandex.net", 80, "http")
    new RemoteHttpService(name = "vos", endpoint = endpoint, client = httpClient)
  }

  private val client = new VosClient(vosHttpService)

  ignore("get offer") {
    Await.result(client.getOffer("1085387206-dc7b2cf3"), 10.seconds)
  }

}
