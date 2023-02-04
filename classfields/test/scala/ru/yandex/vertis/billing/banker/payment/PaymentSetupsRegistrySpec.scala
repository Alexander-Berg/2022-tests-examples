package ru.yandex.vertis.billing.banker.payment

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.billing.banker.model.PaymentSystemIds
import ru.yandex.vertis.billing.banker.model.gens.{PaymentMethodWithCardGen, Producer}
import ru.yandex.vertis.billing.banker.service.PaymentSystemSupport.MethodFilter
import ru.yandex.vertis.billing.banker.service.impl.TrivialPaymentSetupsRegistry
import ru.yandex.vertis.billing.banker.service.{PaymentSetup, PaymentSystemSupport}
import ru.yandex.vertis.billing.banker.util.RequestContext
import ru.yandex.vertis.mockito.MockitoSupport

import scala.concurrent.Future

class PaymentSetupsRegistrySpec
  extends AnyWordSpec
  with Matchers
  with AsyncSpecBase
  with MockitoSupport
  with BeforeAndAfterEach {

  "available" should {
    "return cards from all payment systems" in {

      implicit val mockedRC: RequestContext = mock[RequestContext]

      val trustMethod = PaymentMethodWithCardGen.next.copy(ps = PaymentSystemIds.Trust)

      val yandexKassaV3Method = PaymentMethodWithCardGen.next
        .copy(ps = PaymentSystemIds.YandexKassaV3, properties = trustMethod.properties)

      val trustPaymentSetup = mock[PaymentSetup]
      val trustPaymentSupport = mock[PaymentSystemSupport]
      when(trustPaymentSetup.support).thenReturn(trustPaymentSupport)
      when(trustPaymentSetup.support.psId).thenReturn(PaymentSystemIds.YandexKassaV3)
      when(trustPaymentSupport.getMethods(?, ?)(?)).thenReturn(Future.successful(Seq(trustMethod)))

      val yandexKassaV3PaymentSetup = mock[PaymentSetup]
      val yandexKassaV3PaymentSupport = mock[PaymentSystemSupport]
      when(yandexKassaV3PaymentSetup.support).thenReturn(yandexKassaV3PaymentSupport)
      when(yandexKassaV3PaymentSetup.support.psId).thenReturn(PaymentSystemIds.YandexKassaV3)
      when(yandexKassaV3PaymentSupport.getMethods(?, ?)(?)).thenReturn(Future.successful(Seq(yandexKassaV3Method)))

      val paymentSetupsRegistry =
        new TrivialPaymentSetupsRegistry(Seq(trustPaymentSetup, yandexKassaV3PaymentSetup))(ec)

      val result = paymentSetupsRegistry.available("test-user", MethodFilter.All).futureValue
      result.succs.size shouldBe 2
    }
  }
}
