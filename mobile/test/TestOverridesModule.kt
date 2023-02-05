package ru.yandex.market.test

import android.content.Context
import com.google.gson.JsonObject
import com.yandex.android.startup.identifier.StartupClientIdentifierData
import com.yandex.android.startup.identifier.metricawrapper.MetricaStartupClientIdentifierProvider
import com.yandex.passport.api.PassportApi
import ru.yandex.market.analytics.AnalyticsMetaInfoProducer
import ru.yandex.market.analytics.PulseConfigurator
import ru.yandex.market.analytics.adjust.AdjustTransport
import ru.yandex.market.analytics.adwords.AdWordsTransport
import ru.yandex.market.analytics.appmetrica.AppMetricaTransport
import ru.yandex.market.analytics.firebase.FirebaseTransport
import ru.yandex.market.analytics.frame.FrameMetricsTransport
import ru.yandex.market.analytics.health.MetricaTransport
import ru.yandex.market.analytics.logger.TransportLogger
import ru.yandex.market.base.network.fapi.FapiEndpoint
import ru.yandex.market.base.network.fapi.contract.FapiContract
import ru.yandex.market.base.network.fapi.contract.FapiFileContract
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestContext
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestExecutor
import ru.yandex.market.base.network.fapi.request.executor.FapiRequestResult
import ru.yandex.market.clean.data.fapi.source.cart.CartFapiClient
import ru.yandex.market.clean.data.fapi.source.notification.loyalty.LoyaltyNotificationsFapiClient
import ru.yandex.market.clean.data.fapi.source.notification.settings.MultiNotificationsSettingsFapiClient
import ru.yandex.market.clean.data.fapi.source.referralprogram.ReferralProgramFapiClient
import ru.yandex.market.clean.data.fapi.source.search.SponsoredSearchFapiClient
import ru.yandex.market.clean.data.fapi.source.subscriptions.SubscriptionsFapiClient
import ru.yandex.market.clean.data.repository.postamate.BluetoothAvailability
import ru.yandex.market.clean.domain.config.initializer.FeatureConfigInitializer
import ru.yandex.market.clean.domain.usecase.product.CallClickDaemonUseCaseImpl
import ru.yandex.market.common.banksdk.sdk.YandexBankSdkFacade
import ru.yandex.market.common.experiments.service.ExperimentConfigService
import ru.yandex.market.common.featureconfigs.di.overrideFeatureConfigsModule
import ru.yandex.market.data.cashback.network.client.GrowingCashbackFapiClient
import ru.yandex.market.data.cashback.network.client.details.CashbackDetailsFapiClient
import ru.yandex.market.data.comparison.network.client.ComparisonFapiClient
import ru.yandex.market.data.ecom.question.network.EcomQuestionFapiClient
import ru.yandex.market.data.onboarding.network.client.OnboardingFapiClient
import ru.yandex.market.data.user.network.UserFapiClient
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.di.MockBluetoothAvailability
import ru.yandex.market.di.MockCartFapiClient
import ru.yandex.market.di.MockCashbackDetailsFapiClient
import ru.yandex.market.di.MockComparisonFapiClient
import ru.yandex.market.di.MockComparisonsFapiClient
import ru.yandex.market.di.MockEcomQuestionFapiClient
import ru.yandex.market.di.MockExperimentConfigService
import ru.yandex.market.di.MockFrameMetricsTransport
import ru.yandex.market.di.MockLaunchDelegateImpl
import ru.yandex.market.di.MockLoyaltyNotificationsFapiClient
import ru.yandex.market.di.MockMultiNotificationsSettingsFapiClient
import ru.yandex.market.di.MockPassportApi
import ru.yandex.market.di.MockPulseConfigurator
import ru.yandex.market.di.MockSubscriptionsFapiClient
import ru.yandex.market.di.TestAdWordsTransport
import ru.yandex.market.di.TestAdjustTransport
import ru.yandex.market.di.TestAppMetricaTransport
import ru.yandex.market.di.TestFirebaseTransport
import ru.yandex.market.di.TestMetricaTransport
import ru.yandex.market.di.TestTransportLogger
import ru.yandex.market.di.UiTestDateTimeProvider
import ru.yandex.market.domain.product.usecase.CallClickDaemonUseCase
import ru.yandex.market.feature.launch.LaunchDelegate
import ru.yandex.market.mocks.DefaultTogglesState
import ru.yandex.market.mocks.MockFeatureConfigInitializer
import ru.yandex.market.mocks.State
import ru.yandex.market.mocks.StateFacade
import ru.yandex.market.mocks.local.MockGrowingCashbackFapiClient
import ru.yandex.market.mocks.local.MockOnboardingPromoFapiClient
import ru.yandex.market.mocks.local.MockReferralProgramFapiClient
import ru.yandex.market.mocks.local.MockUserFapiClient
import ru.yandex.market.mocks.local.fapi.MockSponsoredSearchFapiClient
import ru.yandex.market.mocks.local.fapi.TestClickDaemonTransport
import ru.yandex.market.mocks.local.fapi.TestSponsoredRequestsTransport
import ru.yandex.market.mocks.local.mapper.LocalStateMapper
import ru.yandex.market.mocks.local.mapper.MultiNotificationSectionMockMapper
import ru.yandex.market.mocks.local.mapper.MultiNotificationsToggleMockMapper
import ru.yandex.market.mocks.local.mapper.OnboardingMockMapper
import ru.yandex.market.mocks.local.mapper.PassportMapper
import ru.yandex.market.mocks.local.mapper.cashback.GrowingCashbackMockMapper
import ru.yandex.market.mocks.local.mapper.v2.EcomQuestionDtoMapper
import ru.yandex.market.mocks.local.mapper.v2.EcomQuestionOptionsDtoMapper
import ru.yandex.market.mocks.local.mapper.v2.FrontApiEcomQuestionDtoMapper
import ru.yandex.market.mocks.state.IdentifierState
import ru.yandex.market.mocks.tryObtain
import ru.yandex.market.mocks.yabank.MockYandexBankSdkFacade
import ru.yandex.market.test.util.NetworkCallInAndroidTestException
import toxin.definition.DefinitionRegistrar
import toxin.module

fun uiTestOverridesModule(states: List<State>) = module {
    overrides()
    overrideFeatureConfigsModule()
    states(states)
    mappers()
}

private fun DefinitionRegistrar.overrides() {

    factory<ExperimentConfigService>(allowOverride = true) {
        MockExperimentConfigService(
            states = get(),
            mapper = get()
        )
    }

    factory<BluetoothAvailability>(allowOverride = true) {
        MockBluetoothAvailability(
            states = get()
        )
    }

    factory<FeatureConfigInitializer>(allowOverride = true) {
        MockFeatureConfigInitializer(
            stateFacade = get(),
            featureConfigsProvider = get(),
            defaultTogglesState = get(),
        )
    }

    factory<MetricaStartupClientIdentifierProvider>(allowOverride = true) {
        val stateFacade = get<StateFacade>()
        MockMetricaStartupCLientIdentifierProvider(stateFacade.getStates())
    }

    singleton<MetricaTransport>(allowOverride = true) {
        TestMetricaTransport()
    }

    singleton<TransportLogger>(allowOverride = true) {
        TestTransportLogger()
    }

    factory<AnalyticsMetaInfoProducer>(allowOverride = true) {
        MockAnalyticsMetaInfoProducer()
    }

    factory<PassportApi>(allowOverride = true) {
        MockPassportApi(
            statesFacade = get(),
            mapper = get()
        )
    }

    factory<MultiNotificationsToggleMockMapper>() {
        MultiNotificationsToggleMockMapper()
    }

    factory<MultiNotificationSectionMockMapper>() {
        MultiNotificationSectionMockMapper(notificationMockMapper = get())
    }

    singleton<MultiNotificationsSettingsFapiClient>(allowOverride = true) {
        MockMultiNotificationsSettingsFapiClient(stateFacade = get(), notificationsSectionsMockMapper = get())
    }

    factory<AppMetricaTransport>(allowOverride = true) {
        TestAppMetricaTransport()
    }

    factory<AdjustTransport>(allowOverride = true) {
        TestAdjustTransport(
            logger = get()
        )
    }

    factory<AdWordsTransport>(allowOverride = true) {
        TestAdWordsTransport()
    }

    factory<FirebaseTransport>(allowOverride = true) {
        TestFirebaseTransport(
            logger = get()
        )
    }

    factory<DateTimeProvider>(allowOverride = true) {
        UiTestDateTimeProvider(stateFacade = get())
    }

    reusable<FapiRequestExecutor>(allowOverride = true) {
        MockFapiRequestExecutor()
    }

    factory<UserFapiClient>(allowOverride = true) {
        MockUserFapiClient(states = get(), mapper = get())
    }

    factory<DefaultTogglesState>(allowOverride = true) {
        DefaultTogglesState()
    }

    reusable<FrameMetricsTransport>(allowOverride = true) {
        MockFrameMetricsTransport()
    }

    singleton<TestClickDaemonTransport>(allowOverride = true) {
        TestClickDaemonTransport()
    }

    singleton<TestSponsoredRequestsTransport>(allowOverride = true) {
        TestSponsoredRequestsTransport()
    }

    factory<GrowingCashbackFapiClient>(allowOverride = true) {
        MockGrowingCashbackFapiClient(stateFacade = get(), growingCashbackMockMapper = get())
    }

    factory<CashbackDetailsFapiClient>(allowOverride = true) {
        MockCashbackDetailsFapiClient(states = get())
    }

    factory<OnboardingFapiClient>(allowOverride = true) {
        MockOnboardingPromoFapiClient(stateFacade = get(), onboardingMockMapper = get())
    }

    factory<EcomQuestionFapiClient>(allowOverride = true) {
        MockEcomQuestionFapiClient(states = get(), frontApiEcomQuestionMapper = get())
    }

    factory<CartFapiClient>(allowOverride = true) {
        MockCartFapiClient(
            states = get(),
            frontApiCartItemDtoMapper = get(),
            selectedServiceDtoMapper = get(),
            frontApiMergedShowPlaceDtoMapper = get()
        )
    }

    factory<SubscriptionsFapiClient>(allowOverride = true) {
        MockSubscriptionsFapiClient(states = get())
    }

    factory<LoyaltyNotificationsFapiClient>(allowOverride = true) {
        MockLoyaltyNotificationsFapiClient(states = get())
    }

    singleton<PulseConfigurator>(allowOverride = true) {
        MockPulseConfigurator()
    }

    factory<ReferralProgramFapiClient>(allowOverride = true) {
        MockReferralProgramFapiClient(
            states = get(),
        )
    }

    factory<ComparisonFapiClient>(allowOverride = true) {
        MockComparisonFapiClient(states = get())
    }

    factory<LaunchDelegate>(allowOverride = true) {
        MockLaunchDelegateImpl(launchResultDataStore = get(), states = get())
    }

    reusable<YandexBankSdkFacade>(allowOverride = true) {
        MockYandexBankSdkFacade(
            states = get()
        )
    }

    factory<MockComparisonsFapiClient>(allowOverride = true) {
        MockComparisonsFapiClient(
            states = get(),
            comparisonEntitiesDtoMapper = get()
        )
    }

    factory<CallClickDaemonUseCase>(allowOverride = true) {
        CallClickDaemonUseCaseImpl(
            sponsoredSearchFapiClient = get(),
        )
    }

    factory<SponsoredSearchFapiClient>(allowOverride = true) {
        MockSponsoredSearchFapiClient(
            states = get(),
        )
    }
}

private fun DefinitionRegistrar.states(states: List<State>) {
    singleton<StateFacade> { StateFacade(states) }
}

private fun DefinitionRegistrar.mappers() {
    reusable<LocalStateMapper> { LocalStateMapper() }
    reusable<PassportMapper> { PassportMapper() }
    reusable<GrowingCashbackMockMapper> { GrowingCashbackMockMapper() }
    reusable<OnboardingMockMapper> { OnboardingMockMapper() }
    reusable<EcomQuestionOptionsDtoMapper> { EcomQuestionOptionsDtoMapper() }
    reusable<EcomQuestionDtoMapper> { EcomQuestionDtoMapper(ecomQuestionOptionsDtoMapper = get()) }
    reusable<FrontApiEcomQuestionDtoMapper> { FrontApiEcomQuestionDtoMapper(ecomMapper = get()) }
}

private class MockMetricaStartupCLientIdentifierProvider(
    private val states: List<State>
) : MetricaStartupClientIdentifierProvider() {

    override fun requestBlocking(context: Context?): StartupClientIdentifierData {
        return createData(states)
    }

    override fun get(context: Context?): StartupClientIdentifierData {
        return createData(states)
    }

    private fun createData(states: List<State>): StartupClientIdentifierData {
        val state = states.tryObtain<IdentifierState>()
        return object : StartupClientIdentifierData {
            override fun getDeviceIdHash(): String {
                return "device-id-hash"
            }

            override fun isNetworkError(): Boolean {
                return false
            }

            override fun getDeviceId(): String {
                return "device-id"
            }

            override fun hasError(): Boolean {
                return false
            }

            override fun getUuid(): String {
                return state?.uuid?.value ?: "any-uuid"
            }

            override fun getErrorDescription(): String {
                return ""
            }

            override fun getErrorCode(): Int {
                return 0
            }
        }
    }
}

private class MockAnalyticsMetaInfoProducer : AnalyticsMetaInfoProducer {
    override fun appendMetaInfo(jsonObject: JsonObject) {
        // no-op
    }
}

private class MockFapiRequestExecutor : FapiRequestExecutor {

    override fun executeJsonRequest(
        context: FapiRequestContext,
        endpoint: FapiEndpoint,
        apiVersion: String,
        contracts: List<FapiContract<*>>
    ): Map<FapiContract<*>, FapiRequestResult> {
        throw NetworkCallInAndroidTestException(*contracts.toTypedArray())
    }

    override fun executeFileRequest(
        context: FapiRequestContext,
        endpoint: FapiEndpoint,
        apiVersion: String,
        contract: FapiFileContract
    ): FapiRequestResult {
        throw NetworkCallInAndroidTestException(contract)
    }
}
