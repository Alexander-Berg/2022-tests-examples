@file:Suppress("ArchitectureLayersRule")

package ru.yandex.market.clean.domain.usecase.checkout.redux

import com.yandex.payment.sdk.core.data.BankName
import com.yandex.payment.sdk.model.data.PartnerInfo
import com.yandex.payment.sdk.model.data.PaymentOption
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import ru.yandex.market.base.redux.stateobject.asSingleStateObject
import ru.yandex.market.base.redux.store.LegacyCompatibleAppStateStore
import ru.yandex.market.base.redux.store.configureStore
import ru.yandex.market.clean.domain.model.checkout.CheckoutFeaturesConfigurationState
import ru.yandex.market.clean.domain.model.checkout.PaymentInfo
import ru.yandex.market.clean.presentation.feature.oneclick.store.SelectedCard
import ru.yandex.market.common.featureconfigs.models.tinkoffCreditsConfigTestInstance
import ru.yandex.market.domain.installments.model.termSummary_OptionsItemTestInstance
import ru.yandex.market.optional.Optional
import ru.yandex.market.redux.bootstrappers.BootstrappersStore
import ru.yandex.market.redux.checkout.setup.featureconfiguration.IsCreditBrokerAvailableFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.checkout.setup.featureconfiguration.IsStationSubscriptionAvailableFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.FreeDeliveryInfoUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.PaymentInfoUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.UserCardsUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.BoostOutletFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.BucketsRemovalFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.MastercardPromoFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.PlusForNotLoggedInFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.ServiceFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.SupportPhoneNumberFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.TinkoffCreditsFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.dispatchers.checkout.setup.featureconfiguration.YandexBankPromoFeatureUpdateFromLegacyActionDispatcher
import ru.yandex.market.redux.reducers.AppReducer
import ru.yandex.market.redux.states.AppState
import ru.yandex.market.redux.states.CheckoutState
import ru.yandex.market.redux.states.Support

class CheckoutReduxStateInitializationUseCaseTest {

    private val reduxStore = LegacyCompatibleAppStateStore(
        appStateStore = configureStore(AppState()) { reducer = AppReducer() },
        reduxCommonHealthAnalytics = mock()
    )

    private val bootstrappersStore = mock<BootstrappersStore>()

    private val tinkoffConfig = tinkoffCreditsConfigTestInstance()

    private val userCards = listOf(
        PaymentOption(
            id = "testCard",
            account = "testAccount",
            system = "mir",
            bankName = BankName.UnknownBank,
            familyInfo = null,
            partnerInfo = PartnerInfo(isYabankCardOwner = true)
        )
    )

    private val tinkoffCreditsFeatureUpdateActionDispatcher = TinkoffCreditsFeatureUpdateFromLegacyActionDispatcher(
        legacyCompatibleAppStateStore = reduxStore,
        getTinkoffCreditsConfigUseCase = mock { on { execute() } doReturn tinkoffConfig },
        isTinkoffCreditsEnabledUseCase = mock { on { execute() } doReturn Single.just(true) },
    )
    private val mastercardPromoFeatureUpdateActionDispatcher = MastercardPromoFeatureUpdateFromLegacyActionDispatcher(
        legacyCompatibleAppStateStore = reduxStore,
        isMastercardPromoAvailableUseCase = mock { on { execute() } doReturn Single.just(true) },
    )
    private val serviceFeatureUpdateActionDispatcher = ServiceFeatureUpdateFromLegacyActionDispatcher(
        legacyCompatibleAppStateStore = reduxStore,
        summaryServicesCalculationEnabledUseCase = mock { on { execute() } doReturn Single.just(true) },
    )
    private val paymentInfoUpdateActionDispatcher = PaymentInfoUpdateFromLegacyActionDispatcher(
        legacyCompatibleAppStateStore = reduxStore,
        getSelectedCardUseCase = mock {
            on { getSelectedCard() } doReturn Single.just(
                SelectedCard(
                    state = SelectedCard.SelectedCardState.CARD_SELECTED,
                    paymentOption = null,
                )
            )
        },
        selectedTinfoffInstallmentsTermUseCases = mock {
            on { getSelectedOptionSingle() } doReturn Single.just(
                Optional.of(
                    termSummary_OptionsItemTestInstance()
                )
            )
        },
        selectedCreditBrokerTermUseCases = mock {
            on { getSelectedOptionSingle() } doReturn Single.just(
                Optional.of(
                    termSummary_OptionsItemTestInstance()
                )
            )
        },
    )
    private val isStationSubscriptionAvailableFeatureUpdateFromLegacyActionDispatcher =
        IsStationSubscriptionAvailableFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            isStationSubscriptionEnabledUseCase = mock {
                on { execute() } doReturn Single.just(true)
            }
        )

    private val boostOutletFeatureUpdateFromLegacyActionDispatcher = BoostOutletFeatureUpdateFromLegacyActionDispatcher(
        legacyCompatibleAppStateStore = reduxStore,
        boostOutletsFeatureManager = mock {
            on { isEnabled() } doReturn true
        }
    )


    private val freeDeliveryInfoUpdateFromLegacyActionDispatcher =
        FreeDeliveryInfoUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            freeDeliveryInfoRepository = mock(),
            getAuthTokenUseCase = mock {
                on { getCurrentAccountAuthToken() } doReturn Single.just(com.annimon.stream.Optional.empty())
            }
        )

    private val isCreditBrokerAvailableFeatureUpdateFromLegacyActionDispatcher =
        IsCreditBrokerAvailableFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            isCreditBrokerEnabledUseCase = mock {
                on { execute() } doReturn Single.just(true)
            }
        )

    private val isPlusForNotLoggedInAvailableFeatureUpdateFromLegacyActionDispatcher =
        PlusForNotLoggedInFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            isPlusForNotLoggedInEnabledUseCase = mock {
                on { execute() } doReturn Single.just(true)
            }
        )

    private val bucketsRemovalFeatureUpdateFromLegacyActionDispatcher =
        BucketsRemovalFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            checkoutBucketsRemovalRemovalFeatureManager = mock {
                on { isEnabled() } doReturn Single.just(false)
            }
        )

    private val userCardsUpdateFromLegacyActionDispatcher =
        UserCardsUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            getUserCardsUseCase = mock {
                on { execute() } doReturn Single.just(userCards)
            }
        )

    private val yandexBankPromoFeatureUpdateFromLegacyActionDispatcher =
        YandexBankPromoFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            isYandexCardEnabledUseCase = mock {
                on { execute() } doReturn Single.just(true)
            }
        )

    private val phoneNumber = "+79999999999"

    private val supportPhoneNumberFeatureUpdateFromLegacyActionDispatcher =
        SupportPhoneNumberFeatureUpdateFromLegacyActionDispatcher(
            legacyCompatibleAppStateStore = reduxStore,
            getSupportPhoneNumberUseCase = mock {
                on { getSupportPhoneNumber() } doReturn Single.just(phoneNumber)
            }
        )

    private val useCase = CheckoutReduxStateInitializationUseCase(
        bootstrappersStore,
        tinkoffCreditsFeatureUpdateActionDispatcher,
        mastercardPromoFeatureUpdateActionDispatcher,
        serviceFeatureUpdateActionDispatcher,
        paymentInfoUpdateActionDispatcher,
        isStationSubscriptionAvailableFeatureUpdateFromLegacyActionDispatcher,
        freeDeliveryInfoUpdateFromLegacyActionDispatcher,
        boostOutletFeatureUpdateFromLegacyActionDispatcher,
        supportPhoneNumberFeatureUpdateFromLegacyActionDispatcher,
        isPlusForNotLoggedInAvailableFeatureUpdateFromLegacyActionDispatcher,
        isCreditBrokerAvailableFeatureUpdateFromLegacyActionDispatcher,
        bucketsRemovalFeatureUpdateFromLegacyActionDispatcher,
        userCardsUpdateFromLegacyActionDispatcher,
        yandexBankPromoFeatureUpdateFromLegacyActionDispatcher
    )


    @Test
    fun `Use case updates only expected sub states`() {
        useCase.execute().blockingAwait()

        val featuresConfigurationState = CheckoutFeaturesConfigurationState(
            tinkoffCreditsConfig = tinkoffConfig,
            isTinkoffCreditsEnabled = true,
            isSummaryServiceCalculationEnabled = true,
            isMastercardPromoAvailable = true,
            isStationSubscriptionEnabled = true,
            boostOutletEnabled = true,
            isCreditBrokerEnabled = true,
            isPlusForNotLoggedInEnabled = true,
            isBucketsRemovalEnabled = false,
            isYandexBankPromoAvailable = true
        )
        val paymentInfo = PaymentInfo(
            selectedCard = SelectedCard(
                state = SelectedCard.SelectedCardState.CARD_SELECTED,
                paymentOption = null,
            ),
            installmentsSelectedOption = termSummary_OptionsItemTestInstance(),
            paymentInProgress = false,
            creditSelectedOption = termSummary_OptionsItemTestInstance(),
        )
        val reducedState = reduxStore.state
        val expectedState = AppState(
            CheckoutState(
                featuresConfigurationState = featuresConfigurationState.asSingleStateObject(),
                paymentInfo = paymentInfo.asSingleStateObject(),
                helperInfo = Support(
                    supportPhoneNumber = phoneNumber
                ),
                userCards = userCards.asSingleStateObject()
            )
        )

        assertThat(reducedState).isEqualTo(expectedState)
    }
}
