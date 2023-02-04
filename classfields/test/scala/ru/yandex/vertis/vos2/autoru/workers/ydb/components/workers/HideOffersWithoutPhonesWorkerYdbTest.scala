package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers

import org.joda.time.DateTime
import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.auto.api.ApiOfferModel.Category
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.mockito.MockitoSupport.{eq => eqq}
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType
import ru.yandex.vos2.OfferModel.OfferFlag
import ru.yandex.vos2.autoru.dao.old.proxy.OldDbWriter
import ru.yandex.vos2.autoru.model.{AutoruOfferID, TestUtils}
import ru.yandex.vos2.commonfeatures.{FeatureRegistryFactory, FeaturesManager}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class HideOffersWithoutPhonesWorkerYdbTest extends AnyWordSpec with MockitoSupport with Matchers {
  implicit val traced: Traced = Traced.empty

  private val oldDbWriter = mock[OldDbWriter]

  abstract private class Fixture {

    private val featuresRegistry = FeatureRegistryFactory.inMemory()
    private val featuresManager = new FeaturesManager(featuresRegistry)

    val worker = new HideOffersWithoutPhonesWorkerYdb(
      oldDbWriter
    ) with YdbWorkerTestImpl {
      override def features: FeaturesManager = featuresManager
    }
  }

  "shouldProcess" in new Fixture {
    val offer1 = TestUtils.createOffer(dealer = false)
    assert(worker.shouldProcess(offer1.build(), None).shouldProcess)

    val offer2 = TestUtils.createOffer(dealer = true)
    assert(!worker.shouldProcess(offer2.build(), None).shouldProcess)

    val offer3 = TestUtils.createOffer(dealer = false)
    offer3.addFlag(OfferFlag.OF_EXPIRED)
    assert(!worker.shouldProcess(offer3.build(), None).shouldProcess)

    val offer4 = TestUtils.createOffer(dealer = false)
    val phone: String = "79264445566"
    offer4.getOfferAutoruBuilder.getSellerBuilder.addPhoneBuilder().setNumber(phone)
    assert(!worker.shouldProcess(offer4.build(), None).shouldProcess)

    val offer5 = TestUtils.createOffer(dealer = false)
    offer5.getOfferAutoruBuilder
      .addNotificationsBuilder()
      .setType(NotificationType.OFFER_HIDE_NO_PHONES)
      .setTimestampCreate(DateTime.now().getMillis)
    assert(worker.shouldProcess(offer5.build(), None).shouldProcess) // все равно снимаем
  }

  "process" in new Fixture {
    when(oldDbWriter.recallWithoutReason(?, ?, ?, ?)(?)).thenReturn(true)
    val offer1 = TestUtils.createOffer(dealer = false).build()
    val offerId = AutoruOfferID.parse(offer1.getOfferID)
    val result = worker.process(offer1, None)
    verify(oldDbWriter).recallWithoutReason(
      eqq(Some(Category.CARS)),
      eqq(offerId.id),
      eqq(offerId.hash.get),
      eqq(false)
    )(
      any()
    )

  }

}
