//
//  DeeplinkMapTests.swift
//  UITests
//
//  Created by Evgeny Y. Petrov on 10/01/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class DeeplinkMapSearchTests: BaseTestCase {
    func testMapRentPriceHeatmap() {
        APIStubConfigurator.setupPriceRentHeatmapDeeplink_MoscowAndMO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_MoscowAndMO(using: self.dynamicStubs)
        APIStubConfigurator.setupMapSearch_MoscowAndMO(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let expectedPageTitle = "Москва и МО"
        let deeplinkToOpen = "https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/karta/?layer=price-rent"
        let expectedHeatmapTitle = "Цена длительной аренды квартиры"

        self.communicator
            .sendDeeplink(deeplinkToOpen)
        SearchResultsMapSteps()
            .isScreenPresented()
            .hasGeoIntentWithTitle(expectedPageTitle)
            .isSwitchToListButtonTappable()
            .isSwitchToFiltersButtonTappable()
            .tapOnHeatmapLegendButton()
            .isHeatmapDescriptionWithTitle(expectedHeatmapTitle)
    }
}
