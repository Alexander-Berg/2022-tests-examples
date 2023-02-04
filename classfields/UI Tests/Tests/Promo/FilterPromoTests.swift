//
//  FilterPromoTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 12.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig
import YREFiltersModel

final class FilterPromoTests: BaseTestCase {
    func testYandexRentWithModalPromo() {
        let config = ExternalAppConfiguration.configuration(region: .moscowAndRegion,
                                                            filters: .rentApartment(rentTime: .long))
        self.relaunchApp(with: config)

        let list = OfferListSteps.mainList()
        let banner = list.filterPromoBanner(kind: .yandexRent)
        let promo = FilterPromoSteps(kind: .yandexRent)
        let filters = FiltersSteps()

        list
            .isScreenPresented()
        banner
            .isPresented()
            .compareWithScreenshot(identifier: "yandexRent")
            .tapOnBanner()
        promo
            .isPresented()
            .compareWithScreenshot(identifier: "yandexRent")
            .tapOnClose()
        banner
            .tapOnBanner()
        promo
            .isPresented()
            .tapOnOpenFilters()
        filters
            .isScreenPresented()
    }

    func testYandexRentSwitcherInBanner() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = SearchResultsListSteps()
        let filters = FiltersSteps()

        list
            .isScreenPresented()
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
            .toggle()
        list
            .openFilters()
        filters
            .isBoolParameterEnabled(cellAccessibilityIdentifier: FiltersSteps.Identifiers.yandexRentCell)
    }

    func testYandexRentWithEmptyList() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }

    func testYandexRentInSpbRegion() {
        self.relaunchApp(with: .configuration(region: .stPeterburgAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
    }
    
    func testYandexRentInNonMoscowAndSpbRegion() {
        self.relaunchApp(with: .configuration(region: .ekaterinburg, filters: .rentApartment(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .yandexRent)
            .isNotPresented()
    }

    func testYandexRentWithShortRent() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = SearchResultsListSteps()
        let filters = FiltersSteps()

        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
        list
            .openFilters()
        filters
            .isScreenPresented()
            .switchToAction(.rent)
            .switchToCategory(.apartment)
            .tapOnRentTimeButton(.short)
            .submitFilters()
        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isNotPresented()
    }

    func testYandexRentWithNonRentType() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = SearchResultsListSteps()
        let filters = FiltersSteps()

        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
        list
            .openFilters()
        filters
            .isScreenPresented()
            .switchToAction(.buy)
            .switchToCategory(.apartment)
            .submitFilters()
        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isNotPresented()
    }

    func testYandexRentWithNonApartmentType() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .long)))

        let list = SearchResultsListSteps()
        let filters = FiltersSteps()

        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isPresented()
        list
            .openFilters()
        filters
            .isScreenPresented()
            .switchToAction(.rent)
            .switchToCategory(.room)
            .tapOnRentTimeButton(.long)
            .submitFilters()
        list
            .withOfferList()
            .filterPromoBanner(kind: .yandexRent)
            .isNotPresented()
    }
}

extension FilterPromoTests {
    func testNotGrannysRenovationWithModalPromo() {
        let config = ExternalAppConfiguration.configuration(region: .stPeterburgAndRegion, filters: .rentApartment(rentTime: .short))
        self.relaunchApp(with: config)

        let list = OfferListSteps.mainList()
        let banner = list.filterPromoBanner(kind: .notGrannys)
        let promo = FilterPromoSteps(kind: .notGrannys)
        let filters = FiltersSteps()

        list
            .isScreenPresented()
        banner
            .isPresented()
            .compareWithScreenshot(identifier: "notGrannys")
            .tapOnInfo()
        promo
            .isPresented()
            .compareWithScreenshot(identifier: "notGrannys")
            .tapOnClose()
        banner
            .tapOnInfo()
        promo
            .isPresented()
            .tapOnOpenFilters()
        filters
            .isScreenPresented()
    }

    func testNotGrannysRenovationSwitcherInBanner() {
        self.relaunchApp(with: .configuration(region: .stPeterburgAndRegion, filters: .rentApartment(rentTime: .short)))

        let list = SearchResultsListSteps()
        let filters = FiltersSteps()

        list
            .isScreenPresented()
            .withOfferList()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
            .toggle()
        list
            .openFilters()
        filters
            .multipleSelectParameter(with: FiltersSteps.Identifiers.renovationCell, hasValue: "Современный")
    }

    func testNotGrannysRenovationWithEmptyList() {
        self.relaunchApp(with: .configuration(region: .ekaterinburg, filters: .rentApartment(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
    }

    func testNotGrannysRenovationInLongRentNotMoscowAndSpb() {
        self.relaunchApp(with: .configuration(region: .ekaterinburg, filters: .rentApartment(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
    }

    func testNotGrannysRenovationInShortRent() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentApartment(rentTime: .short)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
    }

    func testNotGrannysRenovationInRoomLongRent() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentRoom(rentTime: .long)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
    }

    func testNotGrannysRenovationInRoomShortRent() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .rentRoom(rentTime: .short)))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
    }

    func testNotGrannysRenovationInBuyApartment() {
        self.relaunchApp(with: .configuration(region: .moscowAndRegion, filters: .buyApartment))

        let list = OfferListSteps.mainList()
        list
            .isScreenPresented()
            .filterPromoBanner(kind: .notGrannys)
            .isNotPresented()
    }

    func testNotGrannysRenovationManualHiding() {
        self.relaunchApp(with: .configuration(region: .stPeterburgAndRegion, filters: .rentApartment(rentTime: .short)))

        let list = SearchResultsListSteps()
        let map = SearchResultsMapSteps()

        list
            .isScreenPresented()
            .withOfferList()
            .filterPromoBanner(kind: .notGrannys)
            .isPresented()
            .tapOnHide()
            .isNotPresented()
        list
            .tapOnSwitchToMapButton()
            .isScreenNotPresented()
        map
            .tapOnSwitchToListButton()
        list
            .isScreenPresented()
            .withOfferList()
            .filterPromoBanner(kind: .notGrannys)
            .isNotPresented()
    }
}

extension ExternalAppConfiguration {
    fileprivate enum FiltersStub {
        case buyApartment
        case rentApartment(rentTime: RentTimeValue)
        case rentRoom(rentTime: RentTimeValue)
    }

    fileprivate static func configuration(region: GeoData.RegionData.RegionType,
                                          filters: FiltersStub) -> ExternalAppConfiguration {
        let filtersSnapshot: ExternalFilterSnapshot
        switch filters {
            case .buyApartment:
                filtersSnapshot = .buyApartment(.init())
            case .rentApartment(let rentTime):
                filtersSnapshot = .rentApartment(.init(rentTime: rentTime))
            case .rentRoom(let rentTime):
                filtersSnapshot = .rentRoom(.init(rentTime: rentTime))
        }
        return ExternalAppConfiguration(
            launchCount: 1,
            pushNotificationIntroWasShown: true,
            startupPromoWasShown: true,
            shouldDisplayEmbeddedMainFilters: false,
            mainListOpened: true,
            selectedTabItem: .search,
            isAuthorized: false,
            filtersSnapshot: filtersSnapshot,
            geoData: .customRegion(region)
        )
    }
}
