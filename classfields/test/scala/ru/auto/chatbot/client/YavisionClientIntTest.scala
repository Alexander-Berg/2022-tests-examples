package ru.auto.chatbot.client

import java.io.File

import org.scalatest.FunSuite
import ru.yandex.vertis.commons.http.client.RemoteHttpService.DefaultAsyncClient
import ru.yandex.vertis.commons.http.client.{ApacheHttpClient, HttpClient, HttpEndpoint, LoggedHttpClient, MonitoredHttpClient, RemoteHttpService, RetryHttpClient, TracedHttpClient}

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by Karpenko Maksim (knkmx@yandex-team.ru) on 2019-02-27.
  */
class YavisionClientIntTest extends FunSuite {

  private val httpClient: HttpClient =
    new ApacheHttpClient(DefaultAsyncClient)
      with LoggedHttpClient
      with MonitoredHttpClient
      with TracedHttpClient
      with RetryHttpClient

  private val yavisionttpService: RemoteHttpService = {
    val endpoint: HttpEndpoint = HttpEndpoint("yandex.ru", 443, "https")
    new RemoteHttpService(name = "yavision", endpoint = endpoint, client = httpClient)
  }

  private val yavisionClient = new YavisionClient(yavisionttpService)

  test("recognize number ") {
    val file = new File(getClass.getResource("/license_plate_photo.jpg").getFile)
    val res = Await.result(yavisionClient.recognizeLicensePlate(file), 10.seconds)
    assert(res.head.number === "T435OY77")
  }
}
