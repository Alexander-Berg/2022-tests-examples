package ru.auto.chatbot.manager

import org.scalatest.{FunSuite, Matchers}
import org.scalatest.concurrent.ScalaFutures
import ru.auto.chatbot.client.CatalogClient
import ru.auto.chatbot.model.HumanMarkModelGen
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client.{ApacheHttpClient, HttpClient, HttpEndpoint, LoggedHttpClient, MonitoredHttpClient, RemoteHttpService, RetryHttpClient, TracedHttpClient}

import scala.concurrent.duration._
import scala.concurrent.Await

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-04-17.
  */
class CatalogManagerTest extends FunSuite with Matchers with ScalaFutures {

  test("CatalogManager get human names") {

    val httpClient: HttpClient =
      new ApacheHttpClient(DefaultAsyncClient)
        with LoggedHttpClient
        with MonitoredHttpClient
        with TracedHttpClient
        with RetryHttpClient

    val catalogHttpService: RemoteHttpService = {
      val endpoint: HttpEndpoint = HttpEndpoint("autoru-catalog-api-int.vrts-slb.test.vertis.yandex.net", 80, "http")
      new RemoteHttpService(name = "catalog", endpoint = endpoint, client = httpClient)
    }

    val catalogClient = new CatalogClient(catalogHttpService)
    val manager = new CatalogManager(catalogClient)

    val res = Await.result(manager.getHumanMarkModelGen("KIA", "RIO", 20508999), 10.seconds)

    res shouldBe HumanMarkModelGen("Kia", "Rio", "III Рестайлинг")
  }

}
