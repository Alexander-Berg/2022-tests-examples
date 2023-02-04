//
//  YaRentTenantShowingsTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class YaRentTenantShowingsTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        RentAPIStubConfiguration.setupServiceInfoWithShowings(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }

    func testShowingEntryPointWithoutFlats() {
        RentAPIStubConfiguration.setupEmptyFlats(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesFlatsSectionSteps()
            .isPresented()
            .isShowingsAvailable()
    }

    func testEntryPointHidden() {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs, overrideFlats: false)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()

        InAppServicesFlatsSectionSteps()
            .isPresented()
            .isShowingsNotAvailable()
    }

    func testEmptyView() {
        RentAPIStubConfiguration.setupShowingsEmpty(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isEmptyPresented()
            .tapOnFooter()

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }
    
    func testEmptyViewWithoutOtherOffersFooter() {
        RentAPIStubConfiguration.setupShowingsEmpty(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.geoData = .fallback()
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isEmptyPresented()
            .isFooterHidden()
    }

    func testErrorView() {
        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isErrorPresented()
    }

    func testShowingActions() {
        RentAPIStubConfiguration.setupShowingsWithoutNotifications(using: self.dynamicStubs)

        // Setted up for snippet tap action
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps.tapOnSnippet()

        OfferCardSteps()
            .isOfferCardPresented()
            .tapBackButton()

        firstShowingSteps
            .isRoommatesPresented()
            .tapOnRoommates()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()

        firstShowingSteps
            .isNotificationNotPresented()
    }

    func testContentView() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupShowingsWithoutNotifications(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isContentPresented()
            .tapOnFooter()

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }

    func testContentViewWithAppUpdateNotification() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupShowingsWithAppUpdateNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isContentPresented()
            .isAppUpdateNotificationPresented()

        showingsSteps
            .showing(at: 0)
            .isNotificationPresented(with: "Обновить")
    }

    func testContentWithoutOtherOffersFooter() {
        RentAPIStubConfiguration.setupShowingsWithoutNotifications(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        configuration.geoData = .fallback()
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        showingsSteps
            .isContentPresented()
            .isFooterHidden()
    }

    func testFallbackAction() {
        RentAPIStubConfiguration.setupShowingsWithFallbackNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented()
            .tapOnNotificationAction()

        WrappedBrowserSteps()
            .isEmbeddedBrowserPresented()
    }

    func testHouseUtilitiesAcceptanceAction() {
        RentAPIStubConfiguration.setupShowingsWithHouseUtilitiesAcceptanceNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented(with: "Настроить")
            .tapOnNotificationAction()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
    }

    func testRentContractSigningAction() {
        YaRentContractAPIStubConfiguration.setupRentContract(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupShowingsWithRentContractSigningNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented(with: "Подписать")
            .tapOnNotificationAction()

        YaRentContractSteps()
            .ensurePresented()
    }

    func testPayFirstMonthAction() {
        RentAPIStubConfiguration.setupShowingsWithFirstPaymentNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented(with: "Оплатить")
            .tapOnNotificationAction()

        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
    }

    func testShareWithRoommatesAction() {
        RentAPIStubConfiguration.setupShowingsWithRoommatesSharingNotification(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented(with: "Отправить")
            .tapOnNotificationAction()

        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
    }

    func testDatePicker() {
        RentAPIStubConfiguration.setupShowingsWithEntryDateNotification(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupShowingsTenantCheckInDate(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)

        let showingsSteps = self.openShowings()
        let firstShowingSteps = showingsSteps
            .isContentPresented()
            .showing(at: 0)

        firstShowingSteps
            .isNotificationPresented(with: "Выбрать")
            .tapOnNotificationAction()

        YaRentShowingsDatePickerSteps()
            .isPresented()
            .sendSelectedDate()
            .isNotPresented()
    }

    private func openShowings() -> YaRentTenantShowingsSteps {
        let servicesSteps = InAppServicesSteps()
        servicesSteps
            .isScreenPresented()
            .isContentPresented()
            .tapOnFlatShowings()

        return YaRentTenantShowingsSteps()
    }
}
