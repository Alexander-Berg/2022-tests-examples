package ru.yandex.vertis.billing.dao.impl.jdbc

import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.service.HoldOnlyOrderServiceSpec
import ru.yandex.vertis.billing.service.cached.{CachedHoldOnlyOrderService, NeverExpireCache}
import ru.yandex.vertis.billing.service.impl.HoldOnlyOrderServiceImpl

/**
  * [[HoldOnlyOrderServiceSpec]] with jdbc hold service and cached HoldOnlyOrderService
  *
  * @author zvez
  */
class JdbcCachedHoldOnlyOrderServiceSpec extends JdbcHoldOnlyOrderServiceSpec {

  override val service = new HoldOnlyOrderServiceImpl(orderDao, holdService) with CachedHoldOnlyOrderService {

    val support = new NeverExpireCache
    val serviceNamespace = SupportedServices.RealtyCommercial
  }
}
