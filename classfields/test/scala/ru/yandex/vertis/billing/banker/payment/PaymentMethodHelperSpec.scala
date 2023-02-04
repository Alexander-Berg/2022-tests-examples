package ru.yandex.vertis.billing.banker.payment

import org.scalatest.Inspectors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.banker.AsyncSpecBase
import ru.yandex.vertis.banker.dao.impl.jdbc.JdbcSpecTemplate
import ru.yandex.vertis.billing.banker.dao.impl.jdbc.JdbcPaymentSystemDao
import ru.yandex.vertis.billing.banker.model.{PaymentMethodHelper, PaymentSystemIds}

/**
  * @author ruslansd
  */
class PaymentMethodHelperSpec extends AnyWordSpec with Matchers with JdbcSpecTemplate with AsyncSpecBase {

  private val supportedPsIds =
    // игнорим robokassa, yandexkassa и appstore, т.к. они более не используются для оплат
    (PaymentSystemIds.values - PaymentSystemIds.Overdraft - PaymentSystemIds.Robokassa - PaymentSystemIds.YandexKassa - PaymentSystemIds.AppStore).toSeq

  private val paymentSystemServices = supportedPsIds.map { ps =>
    ps -> new JdbcPaymentSystemDao(database, ps)
  }

  "PaymentMethodHelper" should {

    "provide names for all methods and gates" in {
      Inspectors.forEvery(paymentSystemServices) { case (psId, pss) =>
        val methods = pss.getMethods.futureValue
        methods.filter(_.isEnabled).foreach { m =>
          PaymentMethodHelper.getPaymentMethodName(psId, m.id) should not be None
        }
      }
    }
  }

}
