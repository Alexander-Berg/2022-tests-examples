package ru.yandex.vertis.billing.api

import ru.yandex.vertis.billing.SupportedServices
import ru.yandex.vertis.billing.api.backend.{Backend, BackendRegistry}
import ru.yandex.vertis.billing.service.ComplainStatusCallEnricher
import ru.yandex.vertis.billing.service.async.{
  AsyncArchiveService,
  AsyncAutoruRequisitesService,
  AsyncBalanceApi,
  AsyncBindingService,
  AsyncBootstrapService,
  AsyncCampaignService,
  AsyncClientService,
  AsyncCustomerService,
  AsyncDiscountService,
  AsyncNotifyClientService,
  AsyncOrderService,
  AsyncRoleService,
  AsyncStatisticsService,
  AsyncUserService
}
import ru.yandex.vertis.mockito.MockitoSupport
import ru.yandex.vertis.ops.prometheus.CompositeCollector

/**
  * @author ruslansd
  */
trait MockedApiBackend extends MockitoSupport {

  val backend = MockedApiBackend.backend
  val registry = MockedApiBackend.registry
}

object MockedApiBackend extends MockitoSupport {

  private val balanceApi: AsyncBalanceApi = mock[AsyncBalanceApi]
  private val roleService: AsyncRoleService = mock[AsyncRoleService]
  private val customerService: AsyncCustomerService = mock[AsyncCustomerService]
  private val userService: AsyncUserService = mock[AsyncUserService]
  private val orderService: AsyncOrderService = mock[AsyncOrderService]
  private val clientService: AsyncClientService = mock[AsyncClientService]
  private val campaignService: AsyncCampaignService = mock[AsyncCampaignService]
  private val statisticService: AsyncStatisticsService = mock[AsyncStatisticsService]
  private val bindingService: AsyncBindingService = mock[AsyncBindingService]
  private val discountService: AsyncDiscountService = mock[AsyncDiscountService]
  private val asyncBootstrapService: AsyncBootstrapService = mock[AsyncBootstrapService]
  private val asyncArchiveService: AsyncArchiveService = mock[AsyncArchiveService]
  private val asyncNotifyClientService: AsyncNotifyClientService = mock[AsyncNotifyClientService]
  private val enricher: ComplainStatusCallEnricher = mock[ComplainStatusCallEnricher]
  private val asyncRequisitesService: AsyncAutoruRequisitesService = mock[AsyncAutoruRequisitesService]

  val backend = Backend(
    balanceApi,
    roleService,
    customerService,
    userService,
    orderService,
    clientService,
    campaignService,
    statisticService,
    bindingService,
    discountService,
    Some(asyncBootstrapService),
    asyncArchiveService,
    asyncNotifyClientService,
    enricher,
    Some(asyncRequisitesService),
    SupportedServices.AutoRu
  )

  val registry = Map(SupportedServices.AutoRu -> backend)
}
