package auto.dealers.multiposting.model.test

import auto.dealers.multiposting.model.PaymentType
import auto.dealers.multiposting.model.PaymentType.{Other, Service, ServiceRefund, ServiceUpdate, Tariff, TariffRefund, TariffUpdate}
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, ZSpec, _}

object PaymentTypeSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[_root_.zio.test.environment.TestEnvironment, Any] =
    suite("PaymentType") (
      test("sealed payment type availability of elements") {
        assert(PaymentType.SortedPaymentType)(hasSameElements(PaymentType.values.toList))
      },
      test("sealed payment type order") {
        val expectedOrder = List(
          Tariff,
          TariffUpdate,
          Service,
          ServiceUpdate,
          ServiceRefund,
          TariffRefund,
          Other
        )

        assert(PaymentType.SortedPaymentType)(equalTo(expectedOrder))
      }
    )
}
