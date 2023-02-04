package ru.yandex.vertis.vos2.autoru.workers.ydb.components.workers.notifications.substages

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.AutoruModel.AutoruOffer.Notification.NotificationType.CELEBRITY_SELLER
import ru.yandex.vos2.OfferModel.Offer
import ru.yandex.vos2.UserModel.UserPhone
import ru.yandex.vos2.autoru.InitTestDbs
import ru.yandex.vos2.autoru.model.AutoruModelUtils.AutoruModelRichOffer
import ru.yandex.vos2.autoru.model.TestUtils
import ru.yandex.vos2.commonfeatures.FeaturesManager

class CelebrityOfferEventTest
  extends AnyWordSpec
  with MockitoSupport
  with InitTestDbs
  with Matchers
  with BeforeAndAfterAll {
  implicit val traced: Traced = Traced.empty

  abstract private class Fixture {
    val mockedFeatureManager = mock[FeaturesManager]

    val worker = new CelebrityOfferEvent(
      components.celebrityList,
      mockedFeatureManager
    )
  }

  ("should not process plebs offer") in new Fixture {
    val offer = TestUtils.createOffer()
    assert(!worker.shouldProcess(offer.build(), None).shouldProcess)
  }

  ("should process celebrity offer") in new Fixture {
    val offer1 = TestUtils.createOffer()
    offer1.getUserBuilder.getUserContactsBuilder.setEmail("askshelest@gmail.com")
    assert(worker.shouldProcess(offer1.build(), None).shouldProcess)

    val offer2 = TestUtils.createOffer()
    val phone = UserPhone.newBuilder().setNumber("79851118832")
    offer2.getUserBuilder.getUserContactsBuilder.addPhones(phone)
    assert(worker.shouldProcess(offer2.build(), None).shouldProcess)
  }

  ("has notification") in new Fixture {
    val offer1: Offer.Builder = TestUtils.createOffer()
    val phone = UserPhone.newBuilder().setNumber("79851118832")
    offer1.getUserBuilder.getUserContactsBuilder.addPhones(phone)
    val processed = worker.process(offer1.build(), None).updateOfferFunc.get(offer1.build())

    assert(processed.hasNotificationByType(CELEBRITY_SELLER))
  }

}
