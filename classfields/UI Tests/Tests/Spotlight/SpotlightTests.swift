//
//  SpotlightTests.swift
//  UI Tests
//
//  Created by Timur Guliamov on 04.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import class YREAppConfig.ExternalAppConfiguration

final class SpotlightTests: BaseTestCase {
    func testAppName1() {
        self.performCommonTests(
            typedText: "недвижимость",
            cellTitle: "Яндекс.Недвижимость",
            cellDescription: "Тысячи актуальных объявлений"
        )
    }

    func testAppName2() {
        self.performCommonTests(
            typedText: "яндекс",
            cellTitle: "Яндекс.Недвижимость",
            cellDescription: "Тысячи актуальных объявлений"
        )
    }

    func testBuy() {
        APIStubConfigurator.setupOfferListDeeplink_Spb(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)

        self.performCommonTests(
            typedText: "вторичка",
            cellTitle: "Поможем купить квартиру",
            specificTest: { _ in
                SearchResultsListSteps()
                    .isScreenPresented()
                    .openFilters()
                    .ensureAction(equalTo: .buy)
            }
        )
    }

    func testSell() {
        self.performCommonTests(
            typedText: "продажа",
            cellTitle: "Поможем продать квартиру",
            specificTest: { _ in UserOffersListSteps().isScreenPresented() }
        )
    }

    func testRent() {
        self.performCommonTests(
            typedText: "снять жильё",
            cellTitle: "Поможем снять жильё",
            specificTest: { _ in }
        )
    }

    func testRentOut() {
        self.performCommonTests(
            typedText: "сдать жильё",
            cellTitle: "Поможем сдать жильё",
            specificTest: { _ in }
        )
    }

    func testSites1() {
        self.performCommonTests(
            typedText: "новое",
            cellTitle: "Квартиры в новостройках",
            specificTest: { _ in }
        )
    }

    func testSites2() {
        self.performCommonTests(
            typedText: "жк",
            cellTitle: "Квартиры в новостройках",
            specificTest: { _ in }
        )
    }

    func testCommercial() {
        self.performCommonTests(
            typedText: "Коммерческая",
            cellTitle: "Коммерческая недвижимость",
            specificTest: { _ in }
        )
    }

    func testMortgage() {
        self.performCommonTests(
            typedText: "кредит",
            cellTitle: "Подберём выгодную ипотеку",
            specificTest: { _ in }
        )
    }

    func testYaRent() {
        self.performCommonTests(
            typedText: "аренда",
            cellTitle: "Яндекс.Аренда",
            cellDescription: "Поможем снять или сдать квартиру без головной боли",
            specificTest: { _ in InAppServicesSteps().isScreenPresented() }
        )
    }

    func testEGRN() {
        self.performCommonTests(
            typedText: "ЕГРН",
            cellTitle: "Проверка квартиры по ЕГРН",
            specificTest: { _ in }
        )
    }

    // MARK: - Private

    private func performCommonTests(typedText: String,
                                    cellTitle: String,
                                    cellDescription: String? = nil,
                                    specificTest: ((SpotlightSteps) -> Void)? = nil) {
        let config = ExternalAppConfiguration.commonUITests
        config.isAuthorized = true
        self.relaunchApp(with: config)

        let spotlight = SpotlightSteps()

        spotlight
            .present()
            .typeText(typedText)
            .tapOnCellWithTitle(cellTitle, description: cellDescription)

        specificTest?(spotlight)
    }
}
