package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.baker.util.Protobuf.RichDateTime
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vertis.vos2.autoru.workers.ydb.components.worker.YdbWorker
import ru.yandex.vos2.BasicsModel.CompositeStatus
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.autoru.dao.{ActivationFailedNoPhone, ActivationSuccess}
import ru.yandex.vos2.autoru.dao.proxy.OffersWriter
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager
import ru.yandex.vos2.getNow

class DelayedReactivationWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers with BeforeAndAfterAll {

  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {

    val offerBuilder: Offer.Builder = TestUtils
      .createOffer(getNow, dealer = false)

    val mockFeatureManager: FeaturesManager = mock[FeaturesManager]
    val mockedWriter: OffersWriter = mock[OffersWriter]

    val worker: YdbWorker = new DelayedReactivationWorkerYdb(mockedWriter) with YdbWorkerTestImpl {
      override def features: FeaturesManager = mockFeatureManager
    }
  }

  "should not process draft" in new Fixture {
    assert(!worker.shouldProcess(offerBuilder.build(), state = None).shouldProcess)
  }

  "should process reactivation in past" in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getReactivationDataBuilder
      .setReactivateAt(DateTime.now().minusDays(1).toProtobufTimestamp)
    assert(worker.shouldProcess(offerBuilder.build(), state = None).shouldProcess)
  }

  "should NOT process reactivation in future" in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getReactivationDataBuilder
      .setReactivateAt(DateTime.now().plusDays(1).toProtobufTimestamp)
    assert(!worker.shouldProcess(offerBuilder.build(), state = None).shouldProcess)

  }

  "should reactivate but error in activation" in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getReactivationDataBuilder
      .setReactivateAt(DateTime.now().minusDays(1).toProtobufTimestamp)

    when(mockedWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationFailedNoPhone)
    val processed = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(!processed.getOfferAutoru.getReactivationData.hasReactivateAt)
  }

  "should reactivate" in new Fixture {
    offerBuilder.getOfferAutoruBuilder.getReactivationDataBuilder
      .setReactivateAt(DateTime.now().minusDays(1).toProtobufTimestamp)

    when(mockedWriter.activate(?, ?, ?, ?, ?, ?, ?)(?)).thenReturn(ActivationSuccess(CompositeStatus.CS_ACTIVE))
    val processed = worker.process(offerBuilder.build(), None).updateOfferFunc.get(offerBuilder.build())
    assert(!processed.getOfferAutoru.getReactivationData.hasReactivateAt)
  }

}
