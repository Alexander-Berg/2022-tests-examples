package ru.yandex.vertis.billing.internal_api

import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.internal_api.backend.{Backend, BackendRegistry}
import ru.yandex.vertis.billing.dao.OfferBillingStorageDao
import ru.yandex.vertis.billing.service.AsyncCampaignProvider
import ru.yandex.vertis.billing.service.async.{
  AsyncBootstrapService,
  AsyncCallsResolutionService,
  AsyncHoldOnlyOrderService,
  AsyncKeyValueService,
  AsyncLimitService,
  AsyncMonthlyDiscountService,
  AsyncOrderService,
  AsyncOrderVerificationService,
  AsyncRoleService,
  CallsResolutionHandler
}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.CompositeCollector

/**
  * @author ruslansd
  */
trait MockedInternalApiBackend extends MockitoSupport {

  val backend = MockedInternalApiBackend.backend
  val registry = MockedInternalApiBackend.registry
}

object MockedInternalApiBackend extends MockitoSupport {

  private val offerBillingStorage: Option[OfferBillingStorageDao] = Some(mock[OfferBillingStorageDao])
  private val callResolutionService: Option[AsyncCallsResolutionService] = Some(mock[AsyncCallsResolutionService])
  private val callsResolutionHandler: Option[CallsResolutionHandler] = Some(mock[CallsResolutionHandler])
  private val roleService: AsyncRoleService = mock[AsyncRoleService]
  private val campaignProvider: AsyncCampaignProvider = mock[AsyncCampaignProvider]
  private val holdService: AsyncHoldOnlyOrderService = mock[AsyncHoldOnlyOrderService]
  private val bootstrapService: Option[AsyncBootstrapService] = Some(mock[AsyncBootstrapService])
  private val orderVerificationService: AsyncOrderVerificationService = mock[AsyncOrderVerificationService]
  private val limitService: AsyncLimitService = mock[AsyncLimitService]
  private val orderService: AsyncOrderService = mock[AsyncOrderService]
  private val monthlyDiscountService: AsyncMonthlyDiscountService = mock[AsyncMonthlyDiscountService]
  private val keyValueService: AsyncKeyValueService = mock[AsyncKeyValueService]

  val backend = Backend(
    offerBillingStorage,
    callResolutionService,
    callsResolutionHandler,
    roleService,
    campaignProvider,
    holdService,
    bootstrapService,
    orderVerificationService,
    limitService,
    orderService,
    monthlyDiscountService,
    keyValueService
  )

  val registry: BackendRegistry =
    Map(SupportedServices.AutoRu -> backend)
}
