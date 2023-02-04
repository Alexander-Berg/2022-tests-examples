package ru.yandex.vertis.feedprocessor.autoru.scheduler.mapper

import org.mockito.Mockito
import org.mockito.Mockito.{times, verify}
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.mapper.PicaPicaRetryer.doPicaPicaRequestWithRetries
import ru.yandex.vertis.feedprocessor.services.picapica.PicaPicaClient
import ru.yandex.vertis.feedprocessor.services.picapica.PicaPicaClient.{FailedUpload, Image, OkUpload, Task, TaskResult}
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class PicaPicaRetryerSpec extends StreamTestBase with MockitoSupport with DummyOpsSupport with TestApplication {
  val config = environment.config.getConfig("feedprocessor.autoru")
  val picaPicaClient = mock[PicaPicaClient]

  "PicaPicaRetryer" should {
    "no reties" in {
      Mockito.reset(picaPicaClient)
      when(picaPicaClient.send(?, ?, ?))
        .thenReturn(
          Future.successful(
            TaskResult(results = Map("partitionId" -> Map("id1" -> OkUpload("1"), "id2" -> OkUpload("2"))))
          )
        )

      val result = doPicaPicaRequestWithRetries(
        picaPicaClient,
        3,
        Seq(Task("key", Seq(Image("1", "http://www.ya.ru/1.jpg"), Image("2", "http://www.ya.ru/2.jpg")))),
        "partitionId",
        "20101",
        TaskResult(Map("partitionId" -> Map.empty))
      ).futureValue
      result shouldEqual TaskResult(Map("partitionId" -> Map("id1" -> OkUpload("1"), "id2" -> OkUpload("2"))))
      verify(picaPicaClient, times(1)).send(?, ?, ?)
    }
    "retry once" in {
      Mockito.reset(picaPicaClient)
      when(picaPicaClient.send(?, ?, ?))
        .thenReturn(
          Future.successful(
            TaskResult(results =
              Map("partitionId" -> Map("id1" -> OkUpload("1"), "id2" -> FailedUpload(Some("Timeout"))))
            )
          )
        )
        .thenReturn(Future.successful(TaskResult(results = Map("partitionId" -> Map("id2" -> OkUpload("2"))))))

      val result = doPicaPicaRequestWithRetries(
        picaPicaClient,
        3,
        Seq(Task("key", Seq(Image("1", "http://www.ya.ru/1.jpg"), Image("2", "http://www.ya.ru/2.jpg")))),
        "partitionId",
        "20101",
        TaskResult(Map("partitionId" -> Map.empty))
      ).futureValue
      result shouldEqual TaskResult(Map("partitionId" -> Map("id1" -> OkUpload("1"), "id2" -> OkUpload("2"))))
      verify(picaPicaClient, times(2)).send(?, ?, ?)
    }
    "retry 3 times and failed" in {
      Mockito.reset(picaPicaClient)
      when(picaPicaClient.send(?, ?, ?))
        .thenReturn(
          Future.successful(
            TaskResult(results =
              Map("partitionId" -> Map("id1" -> FailedUpload(Some("Timeout")), "id2" -> FailedUpload(Some("Timeout"))))
            )
          )
        )

      val result = doPicaPicaRequestWithRetries(
        picaPicaClient,
        3,
        Seq(Task("key", Seq(Image("1", "http://www.ya.ru/1.jpg"), Image("2", "http://www.ya.ru/2.jpg")))),
        "partitionId",
        "20101",
        TaskResult(Map("partitionId" -> Map.empty))
      ).futureValue
      result shouldEqual TaskResult(
        Map("partitionId" -> Map("id1" -> FailedUpload(Some("Timeout")), "id2" -> FailedUpload(Some("Timeout"))))
      )
      verify(picaPicaClient, times(3)).send(?, ?, ?)
    }
  }
}
