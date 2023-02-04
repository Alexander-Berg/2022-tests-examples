package ru.yandex.vos2.autoru.letters.renderers

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.junit.JUnitRunner
import ru.yandex.vertis.tracing.Traced
import ru.yandex.vos2.services.promocoder.PromocoderClient
import ru.yandex.vertis.mockito.MockitoSupport._
import org.mockito.Mockito.doNothing
import ru.yandex.vos2.autoru.model.TestUtils

//@Ignore
@RunWith(classOf[JUnitRunner])
class ExternalPanoramaPromocodeSentRendererTest extends AnyFunSuite {
  implicit private val trace = Traced.empty

  private val promocoderClient: PromocoderClient = mock[PromocoderClient]

  /* new HttpPromocoderClient(
      service = "autoru-users",
      hostname = "promocoder-01-sas.test.vertis.yandex.net",
      port = 2000,
      scheme = None,
      hostOverride = None
    )*/
  doNothing().when(promocoderClient).createPromocode(?)(?)
  val renderer = new ExternalPanoramaPromocodeSentRenderer(promocoderClient)

  test("Create notification with email and offerChat template") {

    val user = TestUtils.createUser().setUserRef("user:62320894")
    val contacts = TestUtils.createUserContacts().setEmail("raiv-ixx@yandex-team.ru")
    val offer = TestUtils.createOffer().setUser(user).setUserContacts(contacts).setUserRef("a_62320894").build()
    val notification = renderer.render(offer)
    assert(notification.chatSupport === None)
    assert(notification.mail !== None)
    assert(notification.sms === None)
    assert(notification.push === None)
    assert(notification.offerChat === None)
    assert(notification.notificationCenterChat !== None)
  }
}
