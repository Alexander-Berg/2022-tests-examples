package ru.yandex.vertis.billing.banker.api.base_admin

import ru.yandex.vertis.billing.banker.Domains
import ru.yandex.vertis.billing.banker.backend_admin.{AdminBackend, AdminBackendRegistry}
import ru.yandex.vertis.billing.banker.dao.KeyValueDao
import ru.yandex.vertis.billing.banker.service.{AccountBootstrapService, PaymentSystemMethodRegistry}
import ru.yandex.vertis.mockito.MockitoSupport

/**
  * Mocked Api Backend for testing api routing, marshalling and etc
  *
  * @author ruslansd
  */
trait MockedAdminBackend extends MockitoSupport {

  private val keyValue: KeyValueDao = mock[KeyValueDao]
  private val bootstrap: AccountBootstrapService = mock[AccountBootstrapService]

  private val paymentSystemRegistry: PaymentSystemMethodRegistry =
    mock[PaymentSystemMethodRegistry]

  val backend =
    AdminBackend(keyValue, Some(bootstrap), paymentSystemRegistry)

  val registry = {
    val r = new AdminBackendRegistry
    r.register(Domains.AutoRu, backend)
    r
  }

}
