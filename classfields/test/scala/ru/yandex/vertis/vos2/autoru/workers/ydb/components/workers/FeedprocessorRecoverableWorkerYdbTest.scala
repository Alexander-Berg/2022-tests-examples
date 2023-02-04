package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Section
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.{Offer, OfferFlag, OfferStatusHistoryItem}
import ru.yandex.vos2.autoru.model.TestUtils.createOffer
import ru.yandex.vos2.getNow
import ru.yandex.vos2.model.ModelUtils.RichOfferBuilder

import scala.concurrent.duration.DurationInt
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FeedprocessorRecoverableWorkerYdbTest
  extends AnyWordSpec
  with MockitoSupport
  with Matchers
  with BeforeAndAfterAll {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val worker = new FeedprocessorRecoverableWorkerYdb with YdbWorkerTestImpl
  }

  ("should not process offer without feedprocessor recoverable") in new Fixture {
    val offerBuilder = createOffer(dealer = true)
    addRemovedStatus(offerBuilder)
    assert(!worker.shouldProcess(offerBuilder.build(), None).shouldProcess)
  }

  ("should not process offer feedprocessor_recoverable false") in new Fixture {
    val offerBuilder = createOffer(dealer = true)
    addRemovedStatus(offerBuilder)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(false)
    assert(!worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

  }

  ("should not process private seller offer") in new Fixture {
    val offerBuilder = createOffer(dealer = false)
    addRemovedStatus(offerBuilder)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(true)
    assert(!worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

  }

  ("set feedprocessor_recoverable false after 30 days for new") in new Fixture {
    val now = getNow
    val offerBuilder = createOffer(dealer = true)
    offerBuilder.getOfferAutoruBuilder.setSection(Section.NEW)
    addRemovedStatus(offerBuilder, now - 30.days.toMillis)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(true)
    offerBuilder.putFlag(OfferFlag.OF_DELETED)

    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    val newOffer = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    assert(!newOffer.getOfferAutoru.getFeedprocessorRecoverable)
  }

  ("set feedprocessor_recoverable false after 3 days for used") in new Fixture {
    val now = getNow
    val offerBuilder = createOffer(dealer = true)
    offerBuilder.getOfferAutoruBuilder.setSection(Section.USED)
    addRemovedStatus(offerBuilder, now - 3.days.toMillis)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(true)
    offerBuilder.putFlag(OfferFlag.OF_DELETED)

    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    val newOffer = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    assert(!newOffer.getOfferAutoru.getFeedprocessorRecoverable)
  }

  ("don't set feedprocessor_recoverable false after less 30 days for new") in new Fixture {
    val now = getNow
    val offerBuilder = createOffer(dealer = true)
    offerBuilder.getOfferAutoruBuilder.setSection(Section.NEW)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(true)
    offerBuilder.putFlag(OfferFlag.OF_DELETED)
    addRemovedStatus(offerBuilder, now - 29.days.toMillis)

    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    val result = worker.process(offerBuilder.build(), None)
    assert(result.updateOfferFunc.isEmpty)

    assert(offerBuilder.getOfferAutoru.getFeedprocessorRecoverable)
  }

  ("don't set feedprocessor_recoverable false after less 3 days for used") in new Fixture {
    val now = getNow
    val offerBuilder = createOffer(dealer = true)
    offerBuilder.getOfferAutoruBuilder.setSection(Section.USED)
    offerBuilder.getOfferAutoruBuilder.setFeedprocessorRecoverable(true)
    offerBuilder.putFlag(OfferFlag.OF_DELETED)
    addRemovedStatus(offerBuilder, now - 2.days.toMillis)

    assert(worker.shouldProcess(offerBuilder.build(), None).shouldProcess)

    val result = worker.process(offerBuilder.build(), None)
    assert(result.updateOfferFunc.isEmpty)

    assert(offerBuilder.getOfferAutoru.getFeedprocessorRecoverable)
  }

  private def addRemovedStatus(builder: Offer.Builder, timestamp: Long = getNow - 30.days.toMillis) = {
    builder.addStatusHistory(
      OfferStatusHistoryItem
        .newBuilder()
        .setOfferStatus(CompositeStatus.CS_REMOVED)
        .setTimestamp(timestamp)
    )
  }
}
