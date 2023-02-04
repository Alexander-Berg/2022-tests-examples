package ru.yandex.vertis.feedprocessor.autoru.scheduler.pipeline.sender

import ru.yandex.vertis.feedprocessor.autoru.model.Messages._
import ru.yandex.vertis.feedprocessor.autoru.model.{OfferError, TaskContext}
import ru.yandex.vertis.feedprocessor.autoru.utils.TestExternalOffer
import ru.yandex.vertis.feedprocessor.util.StreamTestBase
import ru.yandex.vertis.feedprocessor.autoru.model.Generators._

import scala.concurrent.duration._

/**
  * @author pnaydenov
  */
class CountersAggregatorStageSpec extends StreamTestBase {
  private val taskContext = TaskContext(newTasksGen.next, 1L)
  private val offer = TestExternalOffer(0, taskContext)
  private val offerMsg = OfferMessage(offer)
  private val offerResultMsg = OfferResultMessage("offer result", offer)
  private val offerFailureMsg = FailureMessage(offer, new OfferError("TEST", "column name", "orig value"), "test")
  private val offerWithCriticalError = FailureMessage(offer, new RuntimeException("CRITICAL ERROR"), "test")
  private val endMsg = StreamEndMessage(taskContext, parsingError = Some(new RuntimeException("TEST PARSING ERROR")))

  "CountersAggregatorStage" should {
    "aggregate" in {
      val (pub, sub) = createPubSub(new CountersAggregatorStage[TestExternalOffer])
      sub.request(4)

      pub.sendNext(offerMsg)
      sub.expectNext() shouldEqual offerMsg
      pub.sendNext(offerResultMsg)
      sub.expectNext() shouldEqual offerResultMsg
      pub.sendNext(offerFailureMsg)
      sub.expectNext() shouldEqual offerFailureMsg
      pub.sendNext(offerWithCriticalError)
      sub.expectNext() shouldEqual offerWithCriticalError
      sub.expectNoMessage(100.millis)

      sub.request(1)
      pub.sendNext(endMsg)
      sub.expectNextPF {
        case StreamEndMessage(taskContext, Some(_), Some(Counters(2, 2, 1))) =>
      }
      sub.expectNoMessage(100.millis)
    }

    "pass StreamEnd message without offers" in {
      val (pub, sub) = createPubSub(new CountersAggregatorStage[TestExternalOffer])
      sub.request(1)
      pub.sendNext(endMsg)
      sub.expectNextPF {
        case StreamEndMessage(taskContext, Some(_), Some(Counters.empty)) =>
      }
      sub.expectNoMessage(100.millis)
    }

    "correctly handle up to maxEntries streames concurrently" in {
      val tc1 = TaskContext(newTasksGen.next, 1L)
      val tc2 = TaskContext(newTasksGen.filter(_.id != tc1.task.id).next, 2L)
      val offer1 = TestExternalOffer(0, tc1)
      val offer2 = TestExternalOffer(0, tc2)

      val (pub, sub) = createPubSub(new CountersAggregatorStage[TestExternalOffer](maxEntries = 2))

      sub.request(2)
      pub.sendNext(OfferMessage(offer1))
      pub.sendNext(OfferMessage(offer2))
      sub.expectNextPF { case OfferMessage(offer1) => }
      sub.expectNextPF { case OfferMessage(offer2) => }

      sub.request(2)
      pub.sendNext(StreamEndMessage(tc2))
      pub.sendNext(StreamEndMessage(tc1))
      sub.expectNextPF {
        case StreamEndMessage(tc2, None, Some(Counters(1, 0, 0))) =>
      }
      sub.expectNextPF {
        case StreamEndMessage(tc1, None, Some(Counters(1, 0, 0))) =>
      }
      sub.expectNoMessage(100.millis)
    }

    "forgot about old streams if maxEntries exceeded" in {
      val tc1 = TaskContext(newTasksGen.next, 1L)
      val tc2 = TaskContext(newTasksGen.filter(_.id != tc1.task.id).next, 2L)
      val tc3 = TaskContext(newTasksGen.filter(t => t.id != tc1.task.id && t.id != tc2.task.id).next, 3L)
      val offer1 = TestExternalOffer(0, tc1)
      val offer2 = TestExternalOffer(0, tc2)
      val offer3 = TestExternalOffer(0, tc3)

      val (pub, sub) = createPubSub(new CountersAggregatorStage[TestExternalOffer](maxEntries = 2))

      sub.request(3)
      pub.sendNext(OfferMessage(offer1))
      pub.sendNext(OfferMessage(offer2))
      pub.sendNext(OfferMessage(offer3))
      sub.expectNextPF { case OfferMessage(offer1) => }
      sub.expectNextPF { case OfferMessage(offer2) => }
      sub.expectNextPF { case OfferMessage(offer3) => }

      sub.request(3)
      pub.sendNext(StreamEndMessage(tc2))
      pub.sendNext(StreamEndMessage(tc1))
      pub.sendNext(StreamEndMessage(tc3))
      sub.expectNextPF {
        case StreamEndMessage(tc2, None, Some(Counters(1, 0, 0))) =>
      }
      sub.expectNextPF {
        case StreamEndMessage(tc1, None, Some(Counters.empty)) =>
      }
      sub.expectNextPF {
        case StreamEndMessage(tc3, None, Some(Counters(1, 0, 0))) =>
      }
      sub.expectNoMessage(100.millis)
    }
  }
}
