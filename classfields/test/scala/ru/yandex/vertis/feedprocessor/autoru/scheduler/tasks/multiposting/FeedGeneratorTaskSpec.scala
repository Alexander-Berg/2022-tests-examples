package ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting

import org.mockito.Mockito.{times, verify}
import ru.yandex.vertis.feedprocessor.app.TestApplication
import ru.yandex.vertis.feedprocessor.autoru.dao.MainOffice7Dao
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.FeedGeneratorTaskSpec.TestClientId
import ru.yandex.vertis.feedprocessor.util.{DummyOpsSupport, StreamTestBase}
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class FeedGeneratorTaskSpec extends StreamTestBase with MockitoSupport with TestApplication with DummyOpsSupport {

  val clientsDao = mock[MainOffice7Dao]
  val feedGeneratorService = mock[FeedGeneratorService]

  val task = new FeedGeneratorTask(clientsDao, feedGeneratorService)

  "FeedGeneratorTask" should {
    "call execute successfully" in {
      when(clientsDao.getMutipostingClientIds).thenReturn(List(TestClientId))
      when(feedGeneratorService.generate(?)).thenReturn(Future.successful(()))

      task.execute().futureValue

      val invocationsServiceCount = times(1)

      verify(clientsDao).getMutipostingClientIds
      verify(feedGeneratorService, invocationsServiceCount).generate(eq(Set(TestClientId)))
    }
  }
}

object FeedGeneratorTaskSpec {
  val TestClientId = 16453L
}
