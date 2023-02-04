//
//  YandexRentTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 08.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class YandexRentTests: BaseTestCase {
    func testOpenYandexRentOffersFromOfferCard() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithoutYandexRent(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupYandexRentDynamicBoundingBoxSpb(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRentTrueSpb(using: self.dynamicStubs)

        self.performCommonTests(regionType: .stPeterburgAndRegion) { cardSteps in
            OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

            cardSteps
                .isOfferCardPresented()
                .scrollToYandexRentPromoBanner()
                .tapYandexRentOffersButton()

            let list = SearchResultsListSteps()
            list
                .isScreenPresented()
                .withOfferList()
                .isListNonEmpty()
                .filterPromoBanner(kind: .yandexRent)
                .isPresented()
        }
    }

    func testNotShowPromoWhenYandexRentIsNotAvailableInLocation() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithoutYandexRent(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupYandexRentDynamicBoundingBoxSpb(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRent_False_Spb(using: self.dynamicStubs)

        self.performCommonTests(regionType: .stPeterburgAndRegion) { cardSteps in
            OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

            cardSteps
                .isOfferCardPresented()
                .isYandexRentPromoBannerNotVisible()
        }
    }

    func testNotShowPromoWhenRegionParamsNotAllowIt() {
        OfferCardAPIStubConfiguration.setupOfferSearchResultsList(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupOfferCardWithoutYandexRent(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupYandexRentDynamicBoundingBoxSpb(using: self.dynamicStubs)
        OfferCardAPIStubConfiguration.setupIsPointInsideRentTrueSpb(using: self.dynamicStubs)

        self.performCommonTests(regionType: .ekaterinburg) { cardSteps in
            OfferCardAPIStubConfiguration.setupOfferSearchResultsListYandexRent(using: self.dynamicStubs)

            cardSteps
                .isOfferCardPresented()
                .isYandexRentPromoBannerNotVisible()
        }
    }

    // MARK: Private

    private func performCommonTests(
        regionType: ExternalAppConfiguration.GeoData.RegionData.RegionType,
        specificCardTests: (OfferCardSteps) -> Void
    ) {
        let configuration = ExternalAppConfiguration.offerCardTests
        configuration.geoData.regionData = .custom(regionType: regionType)
        self.relaunchApp(with: configuration)

        CommonSearchResultsSteps()
            .openFilters()

        FiltersSteps()
            .isScreenPresented()
            .switchToAction(.rent)
            .switchToCategory(.apartment)
            .submitFilters()

        SearchResultsListSteps()
            .isScreenPresented()
            .withOfferList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let offerCardSteps = OfferCardSteps()
        specificCardTests(offerCardSteps)
    }
}
