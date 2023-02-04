//
//  InAppServicesTests.swift
//  UI Tests
//
//  Created by Ella Meltcina on 9/17/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class InAppServicesTests: BaseTestCase {
    func testShowingPromoRentBannerForUnauthorized() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.isAuthorized = false
        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        let bannerSteps = YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .promo)
            .tapOnOwnerButton()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()

        bannerSteps
            .tapOnTenantButton()

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }

    func testNotShowingPromoRentBannerForUnauthorizedFromRegions() {
        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.isAuthorized = false
        configuration.geoData = .fallback()

        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        YandexRentBannerViewSteps()
            .isBannerNotPresented()
    }

    func testShowingPromoRentBanner() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionNotPresented()

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .promo)
    }

    func testNotShowingPromoRentBannerForUserFromRegions() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.geoData = .fallback()

        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        YandexRentBannerViewSteps()
            .isBannerNotPresented()
    }

    func testShowingAccountRentBannerForOwner() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOwner(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .account)
            .tapOnAccountButton()

        WrappedBrowserSteps()
            .isEmbeddedBrowserPresented()
            .closeEmbeddedBrowser()
            .isEmbeddedBrowserNotPresented()
    }

    func testShowingAccountRentBannerForOwnerFromRegions() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOwner(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.geoData = .fallback()

        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .account)
            .tapOnAccountButton()

        WrappedBrowserSteps()
            .isEmbeddedBrowserPresented()
            .closeEmbeddedBrowser()
            .isEmbeddedBrowserNotPresented()
    }

    func testShowingServerError() {
        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .isErrorPresented()
    }

    func testUserBannedInRealty() {
        InAppServicesStubConfigurator.setupBannedServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .isBannedInRealtyPresented()
            .tapOnBannedInRealtyAction()

        ChatSteps()
            .isScreenPresented()
    }

    func testShowingFlatsForTenant() {
        InAppServicesStubConfigurator.setupServiceInfoWithTenant(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesFlatsSectionSteps()
            .isPresented()
            .isCreationAvailable()
            .ensureFlatStatus("снимаю", at: 0)
            .tapOnFormCreation()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
    }

    func testShowingFlatsForOwnerWithoutDrafts() {
        InAppServicesStubConfigurator.setupServiceInfoWithTenant(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithAllOwnerStatuses(using: self.dynamicStubs, withDraftFlats: false)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesFlatsSectionSteps()
            .isPresented()
            .isCreationAvailable()
            .ensureFlatStatus("заявка подтверждена", at: 0)
            .ensureFlatStatus("принята в работу", at: 1)
            .ensureFlatStatus("ищем жильцов", at: 2)
            .ensureFlatStatus("отказались от сотрудничества", at: 3)
            .ensureFlatStatus("заявка отклонена", at: 4)
            .ensureFlatStatus("сдана", at: 5)
            .ensureFlatStatus("аренда окончена", at: 6)
            .tapOnFormCreation()

        YaRentOwnerApplicationSteps()
            .isScreenPresented()
    }

    func testShowingFlatsForOwnerWithDrafts() {
        InAppServicesStubConfigurator.setupServiceInfoWithTenant(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithAllOwnerStatuses(using: self.dynamicStubs, withDraftFlats: true)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesFlatsSectionSteps()
            .isPresented()
            .isCreationNotAvailable()
            .ensureFlatStatus("черновик анкеты", at: 0)
            .ensureFlatStatus("черновик анкеты", at: 1)
            .ensureFlatStatus("заявка подтверждена", at: 2)
            .ensureFlatStatus("принята в работу", at: 3)
            .ensureFlatStatus("ищем жильцов", at: 4)
            .ensureFlatStatus("отказались от сотрудничества", at: 5)
            .ensureFlatStatus("заявка отклонена", at: 6)
            .ensureFlatStatus("сдана", at: 7)
            .ensureFlatStatus("аренда окончена", at: 8)
            .tapOnFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()
    }
    
    func testOpenAddOfferScreen() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnAddOffer()
        
        UserOfferWizardSteps()
            .isScreenPresented()
    }

    func testShowingUserOffers() {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        UserOffersListSteps()
            .isScreenPresented()
    }

    func testShowingUserOffersAsCollection() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferCollection(
            withPromoOffer: true,
            using: self.dynamicStubs
        )

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()
            .ensureTypeOfSnippet(.rentPromo)
            .tapOnOfferAction()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
    }

    func testShowingUserOffersAsList() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupBannedUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsList()
            .ensureStatusIsEqual(to: "0 просмотров сегодня", at: 0)
            .ensureStatusIsEqual(to: "Требует активации", at: 1)
            .ensureStatusIsEqual(to: "0 просмотров сегодня", at: 2)
            .ensureStatusIsEqual(to: "На публикации", at: 3)
            .tapOnOffer(at: 3)

        UserOffersCardSteps()
            .isScreenPresented()
    }

    func testShowingUserOffersAsCollectionWithoutPromo() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferCollection(withPromoOffer: true, using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        let offersCollectionSteps = InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()
            .ensureTypeOfSnippet(.rentPromo)
            .isViewsHistogramNotPresented()

        YandexRentBannerViewSteps()
            .isBannerNotPresented()

        offersCollectionSteps
            .tapOnOfferAction()

        WebPageSteps()
            .screenIsPresented()
    }

    func testShowingUserOffersAsCollectionWithPromo() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferCollection(withPromoOffer: false, using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()
            .ensureTypeOfSnippet(.common)
            .isViewsHistogramNotPresented()

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .promo)
    }

    func testShowingUserOffersAsListWithPromo() {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsList()

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .promo)
    }

    func testShowingUserOffersAsCollectionWithPromoForRentUser() {
        InAppServicesStubConfigurator.setupServiceInfoRentUserWithOffers(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()
            .ensureTypeOfSnippet(.rentPromo)

        YandexRentBannerViewSteps()
            .isBannerPresented()
            .compareCurrentState(with: .account)
    }

    func testUserOfferRaisingAction() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupVASUserOffersCard(using: self.dynamicStubs, stubKind: .common)

        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferCollection(
            withPromoOffer: true,
            using: self.dynamicStubs
        )

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()
            .swipe(.left)
            .ensureTypeOfSnippet(.common)
            .isViewsHistogramPresented()
            .tapOnOfferAction()

        UserOfferProductInfoViewSteps()
            .isScreenPresented()
            .tapActivateButton()
            .isPaymentMethodsScreenPresented()
    }

    func testUserOfferCollectionActions() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferCollectionWithSpecifiedActions(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        let offersCollectionSteps = InAppServicesUserOffersSteps()
            .isSectionPresentedAsCollection()

        // Активирование
        offersCollectionSteps
            .ensureTypeOfSnippet(.common)
            .tapOnOfferAction()

        UserOfferActivationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
        UserOffersCardSteps()
            .tapOnBackButton()

        // Саппорт
        offersCollectionSteps
            .swipe(.left)
            .swipe(.left)
            .ensureTypeOfSnippet(.common)
            .tapOnOfferAction()

        ChatSteps()
            .isScreenPresented()
            .tapBackButton()

        // Редактирование
        offersCollectionSteps
            .swipe(.right)
            .ensureTypeOfSnippet(.common)
            .tapOnOfferAction()

        UserOfferFormSteps()
            .isScreenPresented()
    }

    func testRegionChange() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionList_OrenburgRegion(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        let inAppServicesSteps = InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
        
        let yandexRentBannerSteps = YandexRentBannerViewSteps()
            .isBannerPresented()

        GeoAPIStubConfigurator.setupRegionInfo_OrenburgRegion(using: self.dynamicStubs)

        inAppServicesSteps
            .tapOnRegion()

        RegionSearchSteps()
            .changeRegion(rgid: "426680")
            .isScreenNotPresented()

        yandexRentBannerSteps
            .isBannerNotPresented()
    }

    func testSearchBlock() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        self.relaunchApp(with: .inAppServicesTests)

        let searchBlockSteps = InAppServicesSearchSteps()
        let rootNavigationSteps = RootNavigationSteps()
        let filtersSteps = FiltersSteps()

        // Check that saved and recent searches cells initially don't exist
        searchBlockSteps
            .ensureSavedSearchCellExists(false)
            .ensureRecentSearchCellExists(false)

        // Check open filters and make any search
        searchBlockSteps
            .openFilters()
            .isScreenPresented()
            .switchToCategory(.garage)
            .submitFilters()

        // Save search
        SavedSearchAPIStubConfigurator.setupListing(using: self.dynamicStubs)

        let mapSteps = SearchResultsMapSteps()
        mapSteps.tapOnSubscribeButton()
        let popupSteps = SaveSearchPopupSteps()
        popupSteps
            .screenIsPresented()
            .tapSubmitButton()
            .tapPostponeButtonIfExists()
            .tapDoneButtonIfExists()

        // Return to servies
        rootNavigationSteps.tabBarTapOnHomeItem()

        // Check that saved and recent searches cells exist
        searchBlockSteps
            .ensureSavedSearchCellExists(true)
            .ensureRecentSearchCellExists(true)

        // Check saved search cell
        searchBlockSteps
            .ensureSavedSearchCellExists(true)
            .savedSearchesCountIsEqualTo("99+")
            .tapOnSavedSearchPreset()
            .screenIsPresented()

        // Return to services
        rootNavigationSteps.tabBarTapOnHomeItem()

        // Test recent search test
        searchBlockSteps
            .tapOnRecentSearchPreset()
            .tapOnRecentSearchBottomSheetApplyButton()
            .isSwitchToFiltersButtonTappable() // Ensure that any search results screen was opened.

        // Return to services
        rootNavigationSteps.tabBarTapOnHomeItem()

        // Test filter preset.
        searchBlockSteps
            .tapOnRentPreset()
            .openFilters()
        filtersSteps
            .ensureAction(equalTo: .rent)
            .ensureCategory(equalTo: .apartment)
    }
}
