package ru.yandex.vertis.feedprocessor.autoru.scheduler.kafka

import org.mockito.Mockito.{times, verify}
import org.scalatest.concurrent.ScalaFutures
import ru.auto.cabinet.Multiposting.{MultiPostingEvent, MultiPostingEventType}
import ru.yandex.vertis.feedprocessor.WordSpecBase
import ru.yandex.vertis.feedprocessor.autoru.scheduler.tasks.multiposting.FeedGeneratorService
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class MultipostingEventsProcessorTest extends WordSpecBase with ScalaFutures with MockitoSupport {

  val feedGeneratorService = mock[FeedGeneratorService]
  val processor = new MultipostingEventsProcessor(feedGeneratorService)

  "MultipostingEventsProcessor" should {
    when(feedGeneratorService.generate(?)).thenReturn(Future.successful(()))

    "process" in {
      val invocationsServiceCount = times(1)

      val event = MultiPostingEvent
        .newBuilder()
        .setType(MultiPostingEventType.GENERATE_CLASSIFIEDS_FEED)
        .setUserRef("dealer:1234")
        .build()

      processor.process(Seq(event)).futureValue

      verify(feedGeneratorService, invocationsServiceCount).generate(eq(Set(1234L)))

    }
  }
}
