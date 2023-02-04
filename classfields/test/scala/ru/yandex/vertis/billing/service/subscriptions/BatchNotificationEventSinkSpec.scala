package ru.yandex.vertis.billing.service.subscriptions

import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.billing.model_core.gens.NotificationEventGen
import ru.yandex.vertis.billing.service.subscriptions.BatchNotificationEventSinkSpec.FakeSendingClient
import ru.yandex.vertis.billing.subscriptions.SendingClient
import ru.yandex.vertis.billing.util.SubscriptionsUtils.getDocumentsPortion
import ru.yandex.vertis.subscriptions.MatcherApi

import scala.collection.mutable
import scala.util.{Success, Try}

class BatchNotificationEventSinkSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 50, workers = 3)

  private val EventsCount = 50
  private val BatchSize = 10

  "BatchNotificationAlsoToVosEventSink" should {

    "work correctly" in {
      val gen = Gen.listOfN(EventsCount, NotificationEventGen)
      forAll(gen) { batch =>
        val eventBatches = batch.grouped(BatchSize).toSeq
        val mainSender = FakeSendingClient()

        val sink = new BatchNotificationEventSink(mainSender)

        eventBatches.foreach { batch =>
          sink.flush(batch)
        }

        mainSender.matcherRequests.length shouldBe EventsCount / BatchSize
        mainSender.byteRequests.length shouldBe 0
        val expectedDocumentsPortion = eventBatches.map(getDocumentsPortion)
        val documentsPortion = mainSender.matcherRequests.map(_.getMatchDocuments.getDocumentsPortion)
        documentsPortion should contain theSameElementsAs expectedDocumentsPortion
      }
    }
  }
}

object BatchNotificationEventSinkSpec {

  case class FakeSendingClient() extends SendingClient {

    val matcherRequests = mutable.ArrayBuffer.empty[MatcherApi.Request]

    val byteRequests = mutable.ArrayBuffer.empty[Array[Byte]]

    private val defaultResponse: Try[MatcherApi.Response] = Success(
      MatcherApi.Response
        .newBuilder()
        .setRequestId("lol")
        .build()
    )

    override def send(request: MatcherApi.Request): Try[MatcherApi.Response] = {
      matcherRequests += request
      defaultResponse
    }

    override def send(requestId: String, data: Array[Byte]): Try[MatcherApi.Response] = {
      byteRequests += data
      defaultResponse
    }

  }

}
