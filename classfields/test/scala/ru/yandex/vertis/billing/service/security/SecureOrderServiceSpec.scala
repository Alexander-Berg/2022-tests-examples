package ru.yandex.vertis.billing.service.security

import org.mockito.Mockito.verify
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.{CustomerId, OrderPayment, OrderProperties}
import ru.yandex.vertis.billing.security._
import ru.yandex.vertis.billing.service.OrderService
import ru.yandex.vertis.billing.service.OrderService.ListFilter
import ru.yandex.vertis.billing.util.Page
import ru.yandex.vertis.mockito.MockitoSupport

/**
  *Specs on [[SecureOrderService]] wrapper
  *
  * @author ruslansd
  */
class SecureOrderServiceSpec extends AnyWordSpec with Matchers with MockitoSupport {

  val filter = ListFilter.NoFilter

  val service = mock[OrderService]

  val security = new SecurityManager {
    val security = securityProviderMock
  }

  val secureService = new SecureOrderService(service, security)

  "SecureOrderService" should {
    "create client order only for SuperUser and client" in {
      val clientId = client.id
      val customerId = CustomerId(clientId, None)
      val properties = OrderProperties("properties", None)

      secureService.create(customerId, properties)(SuperUserUid)
      verify(service).create(customerId, properties)(SuperUserUid)

      secureService.create(customerId, properties)(ClientUid)
      verify(service).create(customerId, properties)(ClientUid)

      expectDeny {
        secureService.create(customerId, properties)(AgencyUid).get
      }
    }

    "create agency order for SuperUser and agency" in {
      val agencyId = agency.id
      val clientId = client.id
      val customerId = CustomerId(clientId, Some(agencyId))
      val properties = OrderProperties("properties", None)

      secureService.create(customerId, properties)(SuperUserUid)
      verify(service).create(customerId, properties)(SuperUserUid)

      secureService.create(customerId, properties)(AgencyUid)
      verify(service).create(customerId, properties)(AgencyUid)

      expectDeny {
        secureService.create(customerId, properties)(ClientUid).get
      }
    }

    "list client orders only for SuperUser and client" in {
      val clientId = client.id
      val customerId = CustomerId(clientId, None)
      val slice = Page(0, 10)

      secureService.list(customerId, slice, filter)(SuperUserUid)
      verify(service).list(customerId, slice, filter)(SuperUserUid)

      secureService.list(customerId, slice, filter)(ClientUid)
      verify(service).list(customerId, slice, filter)(ClientUid)

      expectDeny {
        secureService.list(customerId, slice, filter)(AgencyUid).get
      }
    }

    "list agency orders for SuperUser and agency and client (RO)" in {
      val agencyId = agency.id
      val clientId = client.id
      val customerId = CustomerId(clientId, Some(agencyId))
      val slice = Page(0, 10)

      secureService.list(customerId, slice, filter)(SuperUserUid)
      verify(service).list(customerId, slice, filter)(SuperUserUid)

      secureService.list(customerId, slice, filter)(AgencyUid)
      verify(service).list(customerId, slice, filter)(AgencyUid)

      secureService.list(customerId, slice, filter)(ClientUid)
      verify(service).list(customerId, slice, filter)(ClientUid)

      expectDeny {
        secureService.list(customerId, slice, filter)(OtherClientUid).get
      }
    }

    "payment client orders only for SuperUser and client" in {
      val clientId = client.id
      val customerId = CustomerId(clientId, None)
      val orderId = 5
      val properties = OrderPayment(1L)

      secureService.payment(customerId, orderId, properties, None)(SuperUserUid)
      verify(service).payment(customerId, orderId, properties, None)(SuperUserUid)

      secureService.payment(customerId, orderId, properties, None)(ClientUid)
      verify(service).payment(customerId, orderId, properties, None)(ClientUid)

      expectDeny {
        secureService.payment(customerId, orderId, properties, None)(AgencyUid).get
      }
    }

    "payment agency orders for SuperUser and agency" in {
      val agencyId = agency.id
      val clientId = client.id
      val customerId = CustomerId(clientId, Some(agencyId))
      val orderId = 5
      val properties = OrderPayment(1L)

      secureService.payment(customerId, orderId, properties, None)(SuperUserUid)
      verify(service).payment(customerId, orderId, properties, None)(SuperUserUid)

      secureService.payment(customerId, orderId, properties, None)(AgencyUid)
      verify(service).payment(customerId, orderId, properties, None)(AgencyUid)

      expectDeny {
        secureService.payment(customerId, orderId, properties, None)(ClientUid).get
      }
    }
  }
}
