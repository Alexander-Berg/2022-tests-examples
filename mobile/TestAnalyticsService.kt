package ru.yandex.market.di

import ru.yandex.market.activity.searchresult.ClarifyCategoryScrollEvent
import ru.yandex.market.activity.searchresult.specify.events.SpecifyCategoryNavigateEvent
import ru.yandex.market.activity.searchresult.specify.events.SpecifyCategoryScrollEvent
import ru.yandex.market.activity.searchresult.specify.events.SpecifyCategoryVisibleEvent
import ru.yandex.market.analitycs.SimpleAnalyticsService
import ru.yandex.market.analitycs.events.AddReviewButtonClickedEvent
import ru.yandex.market.analitycs.events.AnalyticsEvent
import ru.yandex.market.analitycs.events.CartItemRemovedEvent
import ru.yandex.market.analitycs.events.CartShownEvent
import ru.yandex.market.analitycs.events.CartUpdateItemsEvent
import ru.yandex.market.analitycs.events.DeliveryInfoShownEvent
import ru.yandex.market.analitycs.events.DeliveryTypeShownEvent
import ru.yandex.market.analitycs.events.FilterShownEvent
import ru.yandex.market.analitycs.events.FilterValuesChangedEvent
import ru.yandex.market.analitycs.events.GoToCartClickEvent
import ru.yandex.market.analitycs.events.GoToCartSnackClickedEvent
import ru.yandex.market.analitycs.events.HandleDeepLinkEvent
import ru.yandex.market.analitycs.events.OrderCreated7dEvent
import ru.yandex.market.analitycs.events.OrderCreatedEvent
import ru.yandex.market.analitycs.events.ProductAddReviewButtonClickedEvent
import ru.yandex.market.analitycs.events.ProductCashbackShownEvent
import ru.yandex.market.analitycs.events.ProductFiltersShownEvent
import ru.yandex.market.analitycs.events.ProductReviewsReadMoreButtonClickedEvent
import ru.yandex.market.analitycs.events.ProductReviewsShownEvent
import ru.yandex.market.analitycs.events.SearchBarClickedEvent
import ru.yandex.market.analitycs.events.SearchResultItemsShownEvent
import ru.yandex.market.analitycs.events.SearchResultScreenShownEvent
import ru.yandex.market.analitycs.events.SessionStartedEvent
import ru.yandex.market.analitycs.events.SkuPageOpenEvent
import ru.yandex.market.analitycs.events.SkuReasonsToBuyShowedEvent
import ru.yandex.market.analitycs.events.SkuShownEvent
import ru.yandex.market.analitycs.events.VendorBreadcrumbsNavigationEvent
import ru.yandex.market.analitycs.events.VendorLinkClicked
import ru.yandex.market.analitycs.events.WebEvent
import ru.yandex.market.analitycs.events.addtocart.addgifttocart.AddGiftToCartEvent
import ru.yandex.market.analitycs.events.addtocart.list.ListButtonClickEvent
import ru.yandex.market.analitycs.events.addtocart.list.ListButtonNavigateEvent
import ru.yandex.market.analitycs.events.addtocart.list.ListButtonShownEvent
import ru.yandex.market.analitycs.events.addtocart.search.SearchButtonClickEvent
import ru.yandex.market.analitycs.events.addtocart.search.SearchButtonNavigateEvent
import ru.yandex.market.analitycs.events.addtocart.search.SearchButtonShownEvent
import ru.yandex.market.analitycs.events.addtocart.sku.SkuButtonClickEvent
import ru.yandex.market.analitycs.events.addtocart.sku.SkuButtonNavigateEvent
import ru.yandex.market.analitycs.events.adult.catalog.CatalogAdultAlertEvent
import ru.yandex.market.analitycs.events.adult.product.ProductAdultAlertEvent
import ru.yandex.market.analitycs.events.adult.search.SearchAdultAlertEvent
import ru.yandex.market.analitycs.events.blueset.ProductSetBlockEvent
import ru.yandex.market.analitycs.events.blueset.ProductSetSnippetEvent
import ru.yandex.market.analitycs.events.cart.CartBadCountNotificationEvent
import ru.yandex.market.analitycs.events.cart.CartPackItemDateVisibleEvent
import ru.yandex.market.analitycs.events.cart.CartPackItemVisibleEvent
import ru.yandex.market.analitycs.events.cart.CartPromoCodeClickEvent
import ru.yandex.market.analitycs.events.cart.CartPromoCodeErrorEvent
import ru.yandex.market.analitycs.events.cart.CartPromoCodeSubmitEvent
import ru.yandex.market.analitycs.events.cart.CartSinglePackItemVisibleEvent
import ru.yandex.market.analitycs.events.cart.CartStrategySwitchedEvent
import ru.yandex.market.analitycs.events.cart.EmptyCartLoginEvent
import ru.yandex.market.analitycs.events.cart.EmptyCartOpenPromoHubEvent
import ru.yandex.market.analitycs.events.cashback.CashbackCheckoutFullPriceEvent
import ru.yandex.market.analitycs.events.cashback.CashbackCheckoutOptionSelectEvent
import ru.yandex.market.analitycs.events.cashback.CashbackCheckoutVisibleEvent
import ru.yandex.market.analitycs.events.catalog.CategoryPageVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutActualizeErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutActualizeWarnEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutAddressConfirmClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutApplyDeliveryTypeEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutBoxVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutChangeDeliveryTypeDialogEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutChangeOrderItemsPopUpContinueEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutChangeOrderItemsPopUpNavigateEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutChangePickupPointEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutCommitPaymentClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutContinueNavigateEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutContinueShoppingEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutCreateOrderClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDateCiaVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDebugEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryAddressDialogEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryAddressVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryButtonClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryCoinErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryNextBoxEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliverySubmitEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutDeliveryTypeChangedEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutEditRecipientDialogFragmentEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutLoginButtonClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutLoginNavigateEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutMakeOrderNavigateEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutMakeOrderSubmitErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutMakeOrderSubmitEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutModifyOrderItemsClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutModifyOrderOptionsClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutNavigateTrackingEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandDeliverySelectedEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandInfoAcceptEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandInfoCloseEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandInfoShowEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandInfoVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandSelectedEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutOnDemandTypeShownEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutPaymentErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutPaymentSubmitErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutPickupPointsShownEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutPromocodeErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutPromocodeSubmitEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutRecipientConfirmClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutRecipientInputVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSelectProfileDialogFragmentEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutShowAllPickupPointsClickEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutShownEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSuccessAnyPaymentMethodConfirmationVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSuccessGrossMultiOrderConfirmationVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSuccessGrossOrderConfirmationVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSuccessShownEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSummaryFieldErrorVisibleEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSummaryOrderErrorEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSummaryOrderJustCreatedEvent
import ru.yandex.market.analitycs.events.checkout.CheckoutSummaryPageVisibleEvent
import ru.yandex.market.analitycs.events.checkout.NewCheckoutDeliveryScreenShownEvent
import ru.yandex.market.analitycs.events.checkout.NewCheckoutDeliveryTypeChangedEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddAddressScreenAddAddressNavigateEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddAddressScreenAddAddressSuccessEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddAddressScreenCancelEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddAddressScreenShownEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddressesAddNewNavigateEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddressesChangeNavigateEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutAddressesShownEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressDeleteNavigateEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressDeleteSuccessEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressSaveNavigateEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressSaveSuccessEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressScreenDeleteCancelEvent
import ru.yandex.market.analitycs.events.checkout.addresses.CheckoutEditAddressScreenShownEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutEnrichAddressNextClickEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutEnrichAddressShowEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutMapAddressInputClickEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutMapBringHereClickEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutMapDeliveryTypeSelectedEvent
import ru.yandex.market.analitycs.events.checkout.map.CheckoutMapDeliveryTypeShownEvent
import ru.yandex.market.analitycs.events.checkout.map.SearchMapEnterEvent
import ru.yandex.market.analitycs.events.checkout.map.SearchMapFinishedEvent
import ru.yandex.market.analitycs.events.checkout.map.SearchMapNavigateEvent
import ru.yandex.market.analitycs.events.checkout.map.SearchMapSuggestSelectEvent
import ru.yandex.market.analitycs.events.checkout.map.SearchMapVisibleEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutNewSummaryPageVisibleEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryAddressOptionEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryBoxItemEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryChangePickupPointEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryDeliveryItemEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryPaymentItemEvent
import ru.yandex.market.analitycs.events.checkout.summary.CheckoutSummaryRecipientItemEvent
import ru.yandex.market.analitycs.events.checkout.summary.NewCheckoutOpenScreenTimeEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonListEmptyScreenNavigateEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonListItemNavigateEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonListScreenLoginButtonLoginEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonListVisibleEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenCharacteristicModeChangeEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenEmptyScreenNavigateEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenModelPinEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenModelRemoveEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenModelRestoreEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenModelUnpinEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenPinIntroTooltipVisibleEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenRemoveListEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenReviewsNavigateEvent
import ru.yandex.market.analitycs.events.comparisons.ComparisonMainScreenVisibleEvent
import ru.yandex.market.analitycs.events.comparisons.ProductCharacteristicsComparisonAddEvent
import ru.yandex.market.analitycs.events.comparisons.ProductCharacteristicsComparisonButtonAddEvent
import ru.yandex.market.analitycs.events.comparisons.ProductCharacteristicsComparisonButtonRemoveEvent
import ru.yandex.market.analitycs.events.comparisons.ProductComparisonPopupNavigateEvent
import ru.yandex.market.analitycs.events.comparisons.ProductComparisonPopupRestoreEvent
import ru.yandex.market.analitycs.events.comparisons.ProductComparisonPopupVisibleEvent
import ru.yandex.market.analitycs.events.comparisons.ProductUpperButtonComparisonAddEvent
import ru.yandex.market.analitycs.events.comparisons.ProductUpperButtonComparisonRemoveEvent
import ru.yandex.market.analitycs.events.comparisons.SearchSnippetComparisonTooltipVisibleEvent
import ru.yandex.market.analitycs.events.counter.ListSnippetCartButtonChangeCountEvent
import ru.yandex.market.analitycs.events.counter.PopupCartButtonChangeCountClickEvent
import ru.yandex.market.analitycs.events.counter.SearchSnippetCartButtonChangeCountEvent
import ru.yandex.market.analitycs.events.counter.SkuCartButtonChangedCountClickEvent
import ru.yandex.market.analitycs.events.credit.CartCreditCheckoutNavigateEvent
import ru.yandex.market.analitycs.events.credit.ProductCreditAvailabilityNavigateEvent
import ru.yandex.market.analitycs.events.credit.ProductCreditAvailabilityShownEvent
import ru.yandex.market.analitycs.events.credit.ProductCreditPopupCartNavigateEvent
import ru.yandex.market.analitycs.events.credit.ProductCreditPopupMoreNavigateEvent
import ru.yandex.market.analitycs.events.credit.ProductCreditPopupShownEvent
import ru.yandex.market.analitycs.events.efim.EfimActivateBonusEvent
import ru.yandex.market.analitycs.events.efim.EfimPopupBonusShownEvent
import ru.yandex.market.analitycs.events.efim.EfimPopupErrorEvent
import ru.yandex.market.analitycs.events.efim.EfimScreenShownEvent
import ru.yandex.market.analitycs.events.filtercategory.CategoryFilterNavigateEvent
import ru.yandex.market.analitycs.events.filtercategory.CategoryFilterShownEvent
import ru.yandex.market.analitycs.events.filtercategory.FiltersShownEvent
import ru.yandex.market.analitycs.events.filtercategory.ListFiltersNavigateEvent
import ru.yandex.market.analitycs.events.filtercategory.SearchFiltersNavigateEvent
import ru.yandex.market.analitycs.events.flashsales.SkuFlashTimerVisibleEvent
import ru.yandex.market.analitycs.events.gifts.GiftAllGoodsNavigateEvent
import ru.yandex.market.analitycs.events.gifts.GiftBlockClickedEvent
import ru.yandex.market.analitycs.events.gifts.GiftBlockVisibleEvent
import ru.yandex.market.analitycs.events.gifts.GiftInfoNavigateEvent
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analitycs.events.login.AccountLinkedEvent
import ru.yandex.market.analitycs.events.login.LoginErrorEvent
import ru.yandex.market.analitycs.events.login.LoginNavigateEvent
import ru.yandex.market.analitycs.events.login.LoginSignOutEvent
import ru.yandex.market.analitycs.events.login.LoginSuccessEvent
import ru.yandex.market.analitycs.events.model.ProductGoToMarketButtonClickedEvent
import ru.yandex.market.analitycs.events.model.ProductGoToMarketButtonVisibleEvent
import ru.yandex.market.analitycs.events.morda.widget.WidgetEvent
import ru.yandex.market.analitycs.events.multioffer.AlternativeOfferAddedToCartEvent
import ru.yandex.market.analitycs.events.notifications.CheckoutSuccessPushNotificationNavigateEvent
import ru.yandex.market.analitycs.events.notifications.CheckoutSuccessPushNotificationShownEvent
import ru.yandex.market.analitycs.events.operationalrating.SellerRatingShowEvent
import ru.yandex.market.analitycs.events.orders.OrderPopupCartToCartNavigateEvent
import ru.yandex.market.analitycs.events.orders.OrderPopupCartVisibleEvent
import ru.yandex.market.analitycs.events.pickuppoints.PickupPointsFiltersApplyEvent
import ru.yandex.market.analitycs.events.pickuppoints.PickupPointsFiltersShownEvent
import ru.yandex.market.analitycs.events.pickuppoints.PickupPointsListShownEvent
import ru.yandex.market.analitycs.events.pickuppoints.PickupPointsMapShownEvent
import ru.yandex.market.analitycs.events.pickuppoints.PickupPointsTypeUpdateEvent
import ru.yandex.market.analitycs.events.postamate.PostamateCheckCodeConnectionEvent
import ru.yandex.market.analitycs.events.postamate.SearchPostamateConnectionEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropDisclaimerClickedEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropPopupBuyButtonClickedEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropPopupOfferClickedEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropPopupOfferShownEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropPopupShownEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropShowHowItWorkEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropSnackBarNavigateEvent
import ru.yandex.market.analitycs.events.pricedrop.PriceDropSnackBarShowEvent
import ru.yandex.market.analitycs.events.promocode.ProductPromoCodeEvent
import ru.yandex.market.analitycs.events.question.QuestionAnalyticsEvent
import ru.yandex.market.analitycs.events.question.QuestionListAnalyticsEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersFirstDislikeClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersFirstLikeClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersSecondDislikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersSecondDislikeReportClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersSecondLikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersSecondLikeRateClickEvent
import ru.yandex.market.analitycs.events.rateme.orders.RateMeOrdersShownEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewFirstDislikeClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewFirstLikeClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewSecondDislikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewSecondDislikeReportClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewSecondLikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewSecondLikeRateClickEvent
import ru.yandex.market.analitycs.events.rateme.popup.review.RateMePopupReviewShownEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchFirstDislikeClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchFirstLikeClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchSecondDislikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchSecondDislikeReportClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchSecondLikeLaterClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchSecondLikeRateClickEvent
import ru.yandex.market.analitycs.events.rateme.search.RateMeSearchShownEvent
import ru.yandex.market.analitycs.events.rebrandingannouncement.RebrandingAnnouncementPopupClosedEvent
import ru.yandex.market.analitycs.events.rebrandingannouncement.RebrandingAnnouncementPopupReadMoreEvent
import ru.yandex.market.analitycs.events.rebrandingannouncement.RebrandingAnnouncementPopupVisibleEvent
import ru.yandex.market.analitycs.events.region.NearbyRegionClickEvent
import ru.yandex.market.analitycs.events.region.NearbyRegionManualClickEvent
import ru.yandex.market.analitycs.events.region.NearbyRegionShownEvent
import ru.yandex.market.analitycs.events.region.OnboardingRegionPageVisibleEvent
import ru.yandex.market.analitycs.events.region.RegionAutoClickEvent
import ru.yandex.market.analitycs.events.region.RegionChooseEvent
import ru.yandex.market.analitycs.events.region.RegionOnboardingShownEvent
import ru.yandex.market.analitycs.events.region.RegionPageFirstSelectEvent
import ru.yandex.market.analitycs.events.region.RegionPageSearchTypeFinishedEvent
import ru.yandex.market.analitycs.events.region.RegionPageVisibleEvent
import ru.yandex.market.analitycs.events.region.RegionUndeliverableEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionDialogButtonClickEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionGetCoordinatesBadRegionTypeEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionGetCoordinatesInterruptedEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionGetCoordinatesRegionUndeliverableEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionGetCoordinatesSuccessEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionGetCoordinatesTimeoutEvent
import ru.yandex.market.analitycs.events.region.confirm.ConfirmRegionNearbyUndeliverableEvent
import ru.yandex.market.analitycs.events.region.confirm.GpsRequestTrackingEvent
import ru.yandex.market.analitycs.events.region.confirm.GpsStateEvent
import ru.yandex.market.analitycs.events.region.confirm.GpsTimeoutEvent
import ru.yandex.market.analitycs.events.region.confirm.LocationPermissionRequestEvent
import ru.yandex.market.analitycs.events.region.health.RegionPageRequestSuggestionsExecuteEvent
import ru.yandex.market.analitycs.events.search.ListResultsVisibleEvent
import ru.yandex.market.analitycs.events.search.SearchErrorOpenPromoHubEvent
import ru.yandex.market.analitycs.events.search.SearchResultsVisibleEvent
import ru.yandex.market.analitycs.events.sizetable.SizeTableLinkNavigateEvent
import ru.yandex.market.analitycs.events.sizetable.SizeTableLinkVisibleEvent
import ru.yandex.market.analitycs.events.sizetable.SizeTableSuccessEvent
import ru.yandex.market.analitycs.events.sizetable.SizeTableVisibleEvent
import ru.yandex.market.analitycs.events.smartshopping.CartApplicableCoinsEvent
import ru.yandex.market.analitycs.events.smartshopping.CartSmartCoinShownEvent
import ru.yandex.market.analitycs.events.smartshopping.CartSmartCoinToggleEvent
import ru.yandex.market.analitycs.events.smartshopping.FutureSmartCoinInformationShownEvent
import ru.yandex.market.analitycs.events.smartshopping.FutureSmartCoinShownEvent
import ru.yandex.market.analitycs.events.smartshopping.NewSmartCoinClosedEvent
import ru.yandex.market.analitycs.events.smartshopping.NewSmartCoinShownEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartCoinAutoApplyEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartCoinInformationClosedEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartCoinInformationNavigateEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartCoinInformationShownEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartShoppingInfoShownEvent
import ru.yandex.market.analitycs.events.smartshopping.SmartShoppingNavigateEvent
import ru.yandex.market.analitycs.events.smartshopping.choose.ChoosePopupSelectBonusErrorEvent
import ru.yandex.market.analitycs.events.smartshopping.choose.ChoosePopupSelectBonusEvent
import ru.yandex.market.analitycs.events.smartshopping.choose.ChoosePopupSelectBonusSuccessEvent
import ru.yandex.market.analitycs.events.softupdate.SoftUpdateDownloadedDialogInstallClickedEvent
import ru.yandex.market.analitycs.events.softupdate.SoftUpdateDownloadedDialogShownEvent
import ru.yandex.market.analitycs.events.softupdate.SoftUpdateGoogleDialogConfirmClickedEvent
import ru.yandex.market.analitycs.events.softupdate.SoftUpdateGoogleDialogDismissClickedEvent
import ru.yandex.market.analitycs.events.softupdate.SoftUpdateGoogleDialogShownEvent
import ru.yandex.market.analitycs.events.supplier.SupplierHintVisibleEvent
import ru.yandex.market.analitycs.events.supplier.SupplierNavigateEvent
import ru.yandex.market.analitycs.events.webview.WebViewPageAnalyticsEvent
import ru.yandex.market.analitycs.events.wishlist.WishListAddToCartClickedEvent
import ru.yandex.market.analitycs.events.wishlist.WishListCartButtonNavigate
import ru.yandex.market.filter.allfilters.AllFiltersSubmitEvent

@Deprecated("Используйте новое api (AnalyticFacade/AnalyticEvent)")
@Suppress("DEPRECATION")
class TestAnalyticsService(
    private val analyticsServices: List<SimpleAnalyticsService>
) : ru.yandex.market.analitycs.AnalyticsService {

    val events = mutableListOf<AnalyticsEvent>()

    override fun report(event: CheckoutOnDemandInfoVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandInfoAcceptEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandInfoCloseEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandTypeShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandInfoShowEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandDeliverySelectedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutOnDemandSelectedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSelectProfileDialogFragmentEvent.Open) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSelectProfileDialogFragmentEvent.Confirm) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSelectProfileDialogFragmentEvent.Add) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSelectProfileDialogFragmentEvent.Edit) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditRecipientDialogFragmentEvent.AddNewOpen) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditRecipientDialogFragmentEvent.AddNewSuccess) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditRecipientDialogFragmentEvent.EditOpen) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditRecipientDialogFragmentEvent.EditSuccess) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutChangeDeliveryTypeDialogEvent.Open) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutChangeDeliveryTypeDialogEvent.Confirm) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryAddressDialogEvent.Open) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryAddressDialogEvent.Add) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryAddressDialogEvent.Confirm) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropShowHowItWorkEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropPopupBuyButtonClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GiftBlockVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutRecipientConfirmClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryAddressVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutRecipientInputVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddressConfirmClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionGetCoordinatesRegionUndeliverableEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionGetCoordinatesTimeoutEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionDialogButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionGetCoordinatesSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionGetCoordinatesBadRegionTypeEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionGetCoordinatesInterruptedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GpsStateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GpsRequestTrackingEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ConfirmRegionNearbyUndeliverableEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: LocationPermissionRequestEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GiftBlockClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GiftInfoNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GiftAllGoodsNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartCreditCheckoutNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessPushNotificationNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessPushNotificationShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCreditAvailabilityNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCreditAvailabilityShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCreditPopupCartNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCreditPopupMoreNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCreditPopupShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionPageRequestSuggestionsExecuteEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionPageFirstSelectEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessGrossOrderConfirmationVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessAnyPaymentMethodConfirmationVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessGrossMultiOrderConfirmationVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchBarClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: DeliveryInfoShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GoToCartSnackClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductReviewsShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductReviewsReadMoreButtonClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductFiltersShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductAddReviewButtonClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: AddReviewButtonClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartItemRemovedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(orderCreatedEvent: OrderCreatedEvent) {
        events.add(orderCreatedEvent)
    }

    override fun report(orderCreatedEvent: OrderCreated7dEvent) {
        events.add(orderCreatedEvent)
    }

    override fun report(deliveryTypeShownEvent: DeliveryTypeShownEvent) {
        events.add(deliveryTypeShownEvent)
    }

    override fun report(event: SearchResultScreenShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(widgetEvent: WidgetEvent) {
        events.add(widgetEvent)
    }

    override fun report(event: SkuShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SessionStartedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchResultItemsShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutCreateOrderClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutCommitPaymentClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutPickupPointsShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutShowAllPickupPointsClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutModifyOrderOptionsClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutModifyOrderItemsClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutLoginButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryOrderErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SpecifyCategoryNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SpecifyCategoryVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SpecifyCategoryScrollEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ClarifyCategoryScrollEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: HealthEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionOnboardingShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionChooseEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionUndeliverableEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: NewSmartCoinClosedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: NewSmartCoinShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartCoinInformationShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: FutureSmartCoinInformationShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: FutureSmartCoinShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartSmartCoinToggleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartSmartCoinShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartShoppingNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartCoinInformationClosedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartCoinInformationNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartShoppingInfoShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SmartCoinAutoApplyEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchFirstLikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchFirstDislikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchSecondDislikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchSecondDislikeReportClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchSecondLikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeSearchSecondLikeRateClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryFieldErrorVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersFirstLikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersFirstDislikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersSecondDislikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersSecondDislikeReportClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersSecondLikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMeOrdersSecondLikeRateClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewFirstLikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewFirstDislikeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewSecondDislikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewSecondDislikeReportClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewSecondLikeLaterClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RateMePopupReviewSecondLikeRateClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchButtonNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchButtonShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListButtonNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListButtonShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuButtonClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuButtonNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutChangeOrderItemsPopUpContinueEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutChangeOrderItemsPopUpNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMakeOrderSubmitEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMakeOrderNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMakeOrderSubmitErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutLoginNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GoToCartClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuReasonsToBuyShowedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryTypeChangedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: NewCheckoutDeliveryTypeChangedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: NewCheckoutDeliveryScreenShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutChangePickupPointEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliverySubmitEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutContinueNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutActualizeErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutActualizeWarnEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutApplyDeliveryTypeEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryNextBoxEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutNavigateTrackingEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutContinueShoppingEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutPaymentErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutPromocodeSubmitEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutPromocodeErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutPaymentSubmitErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(regionAutoClickEvent: RegionAutoClickEvent) {
        events.add(regionAutoClickEvent)
    }

    override fun report(nearbyRegionClickEvent: NearbyRegionClickEvent) {
        events.add(nearbyRegionClickEvent)
    }

    override fun report(nearbyRegionManualClickEvent: NearbyRegionManualClickEvent) {
        events.add(nearbyRegionManualClickEvent)
    }

    override fun report(nearbyRegionShownEvent: NearbyRegionShownEvent) {
        events.add(nearbyRegionShownEvent)
    }

    override fun report(event: CheckoutSummaryOrderJustCreatedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: WishListAddToCartClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: WishListCartButtonNavigate) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CatalogAdultAlertEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductAdultAlertEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchAdultAlertEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddAddressScreenAddAddressNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddAddressScreenAddAddressSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddAddressScreenCancelEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddAddressScreenShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddressesAddNewNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressSaveSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressScreenShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressScreenDeleteCancelEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressSaveNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressDeleteSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEditAddressDeleteNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddressesShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutAddressesChangeNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuPageOpenEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListResultsVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchResultsVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDateCiaVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSuccessShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropDisclaimerClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropPopupOfferShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropPopupOfferClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropSnackBarShowEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropSnackBarNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: OrderPopupCartVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: OrderPopupCartToCartNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: LoginNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: LoginSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: LoginErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: LoginSignOutEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: AccountLinkedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PriceDropPopupShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: VendorBreadcrumbsNavigationEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: WebEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CategoryFilterNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CategoryFilterShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: FiltersShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListFiltersNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchFiltersNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: OnboardingRegionPageVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionPageSearchTypeFinishedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RegionPageVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SoftUpdateDownloadedDialogInstallClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SoftUpdateDownloadedDialogShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SoftUpdateGoogleDialogConfirmClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SoftUpdateGoogleDialogDismissClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SoftUpdateGoogleDialogShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PickupPointsMapShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PickupPointsListShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PickupPointsTypeUpdateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PickupPointsFiltersShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PickupPointsFiltersApplyEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryPageVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: AllFiltersSubmitEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartPromoCodeClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartPromoCodeSubmitEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartPromoCodeErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchErrorOpenPromoHubEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EmptyCartOpenPromoHubEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EmptyCartLoginEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: HandleDeepLinkEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartUpdateItemsEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchMapVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchMapFinishedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchMapEnterEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchMapSuggestSelectEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchMapNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CategoryPageVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: VendorLinkClicked) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: GpsTimeoutEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SizeTableVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SizeTableSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SizeTableLinkNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SizeTableLinkVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: FilterShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: FilterValuesChangedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchPostamateConnectionEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PostamateCheckCodeConnectionEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: AlternativeOfferAddedToCartEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDebugEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutBoxVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutDeliveryCoinErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EfimScreenShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EfimPopupBonusShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EfimActivateBonusEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: EfimPopupErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuFlashTimerVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductSetBlockEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductSetSnippetEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }


    override fun report(event: ChoosePopupSelectBonusEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ChoosePopupSelectBonusSuccessEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ChoosePopupSelectBonusErrorEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutNewSummaryPageVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryDeliveryItemEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryRecipientItemEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: NewCheckoutOpenScreenTimeEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryPaymentItemEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryBoxItemEvent.Visible) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryAddressOptionEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutSummaryChangePickupPointEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SellerRatingShowEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: QuestionListAnalyticsEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: QuestionAnalyticsEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartPackItemVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartPackItemDateVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartSinglePackItemVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductGoToMarketButtonVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductGoToMarketButtonClickedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductUpperButtonComparisonAddEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductUpperButtonComparisonRemoveEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductComparisonPopupVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductComparisonPopupNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductComparisonPopupRestoreEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonListVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonListItemNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenModelPinEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenModelUnpinEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenModelRemoveEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenModelRestoreEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenCharacteristicModeChangeEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonListEmptyScreenNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenReviewsNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenEmptyScreenNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenPinIntroTooltipVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonMainScreenRemoveListEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCharacteristicsComparisonButtonAddEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCharacteristicsComparisonAddEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCharacteristicsComparisonButtonRemoveEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchSnippetComparisonTooltipVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ComparisonListScreenLoginButtonLoginEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SkuCartButtonChangedCountClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: PopupCartButtonChangeCountClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ListSnippetCartButtonChangeCountEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SearchSnippetCartButtonChangeCountEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartBadCountNotificationEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartApplicableCoinsEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CartStrategySwitchedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductPromoCodeEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RebrandingAnnouncementPopupVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RebrandingAnnouncementPopupReadMoreEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: RebrandingAnnouncementPopupClosedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMapDeliveryTypeShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMapDeliveryTypeSelectedEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMapAddressInputClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutMapBringHereClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEnrichAddressShowEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CheckoutEnrichAddressNextClickEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CashbackCheckoutVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CashbackCheckoutOptionSelectEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: CashbackCheckoutFullPriceEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SupplierNavigateEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: SupplierHintVisibleEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: ProductCashbackShownEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: AddGiftToCartEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }

    override fun report(event: WebViewPageAnalyticsEvent) {
        events.add(event)
        analyticsServices.forEach { service -> service.report(event) }
    }
}
