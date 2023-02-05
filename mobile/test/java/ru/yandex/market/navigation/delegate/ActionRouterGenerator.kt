package ru.yandex.market.navigation.delegate

import com.yandex.div.core.Div2Context
import flex.actions.ActionRouter
import flex.content.sections.gallery.GalleryScrollResultProvider
import org.mockito.kotlin.mock
import ru.yandex.market.activity.order.GetOfferUseCase
import ru.yandex.market.activity.order.RepeatOrderUseCase
import ru.yandex.market.clean.data.mapper.DeliveryDateTimeIntervalMapper
import ru.yandex.market.clean.data.mapper.DeliveryTimeIntervalMapper
import ru.yandex.market.clean.data.mapper.OrderItemInfoMapper
import ru.yandex.market.clean.data.mapper.cart.CartItemMapper
import ru.yandex.market.clean.domain.usecase.GetSupportChatUrlUseCase
import ru.yandex.market.clean.domain.usecase.cart.GetOfferFromCacheOrRemoteUseCase
import ru.yandex.market.clean.domain.usecase.checkout.ResetCheckoutFlowUseCase
import ru.yandex.market.clean.domain.usecase.lavka.OnDemandCourierLinkUseCase
import ru.yandex.market.clean.domain.usecase.livestream.ResolveLiveStreamScreenUseCase
import ru.yandex.market.clean.domain.usecase.order.ChatConfigUseCase
import ru.yandex.market.clean.domain.usecase.order.GetCourierTrackingUrlUseCase
import ru.yandex.market.clean.domain.usecase.order.GetOrderConsultationUnreadMessageCountUseCase
import ru.yandex.market.clean.domain.usecase.order.GetOrderUseCase
import ru.yandex.market.clean.domain.usecase.order.ValidateOrderUseCase
import ru.yandex.market.clean.domain.usecase.orderfeedback.SetOrderDeliveryFeedbackUseCase
import ru.yandex.market.clean.domain.usecase.pickup.renewal.CanShowPickupRenewalButtonUseCase
import ru.yandex.market.clean.domain.usecase.plus.PlusForNotLoggedInEnabledUseCase
import ru.yandex.market.clean.domain.usecase.stockdate.GetProductsDateInStockUseCase
import ru.yandex.market.clean.presentation.feature.bnpl.BnplDialogScreenProvider
import ru.yandex.market.clean.presentation.feature.cashback.about.AboutCashbackInfoTypeArgumentMapper
import ru.yandex.market.clean.presentation.feature.cashback.details.CashbackDetailsFormatter
import ru.yandex.market.clean.presentation.feature.cms.item.instruction.InstructionFormatter
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandCourierScreenManager
import ru.yandex.market.clean.presentation.feature.ondemand.OnDemandHealthFacade
import ru.yandex.market.clean.presentation.formatter.SpecificationsFormatter
import ru.yandex.market.clean.presentation.navigation.RouterFactory
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.data.deeplinks.params.resolver.MapUrlToDeeplinkUseCase
import ru.yandex.market.data.media.image.mapper.ImageReferenceMapper
import ru.yandex.market.domain.fintech.usecase.yacard.IsYaCardEnabledUseCase
import ru.yandex.market.domain.service.usecase.GetLastSelectedServiceForOfferUseCase
import ru.yandex.market.domain.user.usecase.HasYandexPlusUseCase
import ru.yandex.market.feature.ar.Open3DPreviewUseCase
import ru.yandex.market.navigation.router.FlexActionRouterProvider
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.util.manager.InstalledApplicationManager
import toxin.Provider

internal class ActionRouterGenerator {

    private val schedulers = presentationSchedulersMock()

    fun generate(
        routerFactory: RouterFactory = mock(),
        mapUrlToDeeplinkUseCase: Provider<MapUrlToDeeplinkUseCase> = mock(),
        resolveLiveStreamScreenUseCase: ResolveLiveStreamScreenUseCase = mock(),
        getOrderUseCase: Lazy<GetOrderUseCase> = mock(),
        canShowPickupRenewalButtonUseCase: Lazy<CanShowPickupRenewalButtonUseCase> = mock(),
        onDemandCourierLinkUseCase: Lazy<OnDemandCourierLinkUseCase> = mock(),
        getCourierTrackingUrlUseCase: Lazy<GetCourierTrackingUrlUseCase> = mock(),
        onDemandCourierScreenManager: OnDemandCourierScreenManager = mock(),
        onDemandHealthFacade: OnDemandHealthFacade = mock(),
        deliveryTimeIntervalMapper: DeliveryTimeIntervalMapper = mock(),
        deliveryDateTimeIntervalMapper: DeliveryDateTimeIntervalMapper = mock(),
        installedApplicationManager: InstalledApplicationManager = mock(),
        resourcesManager: ResourcesManager = mock(),
        repeatOrderUseCase: Lazy<RepeatOrderUseCase> = mock(),
        orderItemInfoMapper: OrderItemInfoMapper = mock(),
        setOrderDeliveryFeedbackUseCase: Lazy<SetOrderDeliveryFeedbackUseCase> = mock(),
        imageReferenceMapper: Lazy<ImageReferenceMapper> = mock(),
        galleryScrollResultProvider: GalleryScrollResultProvider = mock(),
        open3DPreviewUseCase: Lazy<Open3DPreviewUseCase> = mock(),
        cartItemMapper: Lazy<CartItemMapper> = mock(),
        validateOrderUseCase: Lazy<ValidateOrderUseCase> = mock(),
        getOfferUseCase: Lazy<GetOfferUseCase> = mock(),
        getProductsDateInStockUseCase: Lazy<GetProductsDateInStockUseCase> = mock(),
        resetCheckoutFlowUseCase: Lazy<ResetCheckoutFlowUseCase> = mock(),
        getLastSelectedServiceForOfferUseCase: Lazy<GetLastSelectedServiceForOfferUseCase> = mock(),
        chatConfigUseCase: Lazy<ChatConfigUseCase> = mock(),
        getSupportChatUrlUseCase: Lazy<GetSupportChatUrlUseCase> = mock(),
        instructionFormatter: InstructionFormatter = mock(),
        specificationsFormatter: SpecificationsFormatter = mock(),
        getOfferFromCacheOrRemoteUseCase: Lazy<GetOfferFromCacheOrRemoteUseCase> = mock(),
        plusForNotLoggedInEnabledUseCase: Lazy<PlusForNotLoggedInEnabledUseCase> = mock(),
        hasYandexPlusUseCase: Lazy<HasYandexPlusUseCase> = mock(),
        isYaCardEnabledUseCase: Lazy<IsYaCardEnabledUseCase> = mock(),
        bnplDialogScreenProvider: Lazy<BnplDialogScreenProvider> = mock(),
        cashbackDetailsFormatter: CashbackDetailsFormatter = mock(),
        aboutCashbackInfoTypeArgumentMapper: AboutCashbackInfoTypeArgumentMapper = mock(),
        div2Context: Lazy<Div2Context> = mock(),
        getOrderConsultationUnreadMessageCountUseCase: Lazy<GetOrderConsultationUnreadMessageCountUseCase> = mock(),
    ): ActionRouter {
        return FlexActionRouterProvider(
            routerFactory = routerFactory,
            mapUrlToDeeplinkUseCase = mapUrlToDeeplinkUseCase,
            resolveLiveStreamScreenUseCase = resolveLiveStreamScreenUseCase,
            getOrderUseCase = getOrderUseCase,
            canShowPickupRenewalButtonUseCase = canShowPickupRenewalButtonUseCase,
            onDemandCourierLinkUseCase = onDemandCourierLinkUseCase,
            getCourierTrackingUrlUseCase = getCourierTrackingUrlUseCase,
            onDemandCourierScreenManager = onDemandCourierScreenManager,
            onDemandHealthFacade = onDemandHealthFacade,
            deliveryTimeIntervalMapper = deliveryTimeIntervalMapper,
            deliveryDateTimeIntervalMapper = deliveryDateTimeIntervalMapper,
            installedApplicationManager = installedApplicationManager,
            resourcesManager = resourcesManager,
            repeatOrderUseCase = repeatOrderUseCase,
            orderItemInfoMapper = orderItemInfoMapper,
            setOrderDeliveryFeedbackUseCase = setOrderDeliveryFeedbackUseCase,
            schedulers = schedulers,
            imageReferenceMapper = imageReferenceMapper,
            galleryScrollResultProvider = galleryScrollResultProvider,
            open3DPreviewUseCase = open3DPreviewUseCase,
            cartItemMapper = cartItemMapper,
            validateOrderUseCase = validateOrderUseCase,
            getOfferUseCase = getOfferUseCase,
            getProductsDateInStockUseCase = getProductsDateInStockUseCase,
            resetCheckoutFlowUseCase = resetCheckoutFlowUseCase,
            getLastSelectedServiceForOfferUseCase = getLastSelectedServiceForOfferUseCase,
            chatConfigUseCase = chatConfigUseCase,
            getSupportChatUrlUseCase = getSupportChatUrlUseCase,
            instructionFormatter = instructionFormatter,
            specificationsFormatter = specificationsFormatter,
            getOfferFromCacheOrRemoteUseCase = getOfferFromCacheOrRemoteUseCase,
            plusForNotLoggedInEnabledUseCase = plusForNotLoggedInEnabledUseCase,
            hasYandexPlusUseCase = hasYandexPlusUseCase,
            isYaCardEnabledUseCase = isYaCardEnabledUseCase,
            cashbackDetailsFormatter = cashbackDetailsFormatter,
            aboutCashbackInfoTypeArgumentMapper = aboutCashbackInfoTypeArgumentMapper,
            bnplDialogScreenProvider = bnplDialogScreenProvider,
            getOrderConsultationUnreadMessageCountUseCase = getOrderConsultationUnreadMessageCountUseCase,
            div2Context = div2Context
        ).getRouter()
    }
}
