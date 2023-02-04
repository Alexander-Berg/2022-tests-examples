//
//  SavedSearchesFormattersTests.swift
//  Unit Tests
//
//  Created by Anfisa Klisho on 16.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum Typograf.SpecialSymbol

final class SavedSearchesFormattersTests: XCTestCase {

    // 1
    func testBuyNewBuildingApartment() {
        let query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["APARTMENT"],
            QueryFilterParameters.objectType: ["NEWBUILDING"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["10000"],
            QueryFilterParameters.priceType: ["PER_METER"],
            QueryFilterParameters.roomsTotal: ["1", "2", "PLUS_4"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Квартира, новостройка, 1, 2, 4+ комн., от 100 до 10 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за м²"
        
        XCTAssertEqual(description, expectedDescription)
    }

    // 2
    func testBuyResaleApartment() {
        let query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["APARTMENT"],
            QueryFilterParameters.objectType: ["OFFER"],
            QueryFilterParameters.newFlat: ["NO"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["10000"],
            QueryFilterParameters.priceType: ["PER_OFFER"],
            QueryFilterParameters.roomsTotal: ["STUDIO", "1"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Квартира, вторичка, студия, 1 комн., от 100 до 10 тыс.\(SpecialSymbol.nbsp)₽"

        XCTAssertEqual(description, expectedDescription)
    }

    // 3
    func testBuyRoom() {
        let query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["ROOMS"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["30000"],
            QueryFilterParameters.priceType: ["PER_OFFER"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Комната, от 100 до 30 тыс.\(SpecialSymbol.nbsp)₽"

        XCTAssertEqual(description, expectedDescription)
    }

    // 4
    func testBuyVillageHouse() {
        var query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["HOUSE"],
            QueryFilterParameters.objectType: ["VILLAGE"]
        ]

        for villageOfferType in self.villageOfferTypes {
            query[QueryFilterParameters.villageOfferType] = [villageOfferType]

            let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
            let expectedDescription = "КП, \(villageOfferTypeLocalizationRule[villageOfferType] ?? "")"

            XCTAssertEqual(description, expectedDescription)
        }
    }

    // 5
    func testBuyResaleHouse() {
        let query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["HOUSE"],
            QueryFilterParameters.objectType: ["OFFER"],
            QueryFilterParameters.primarySale: ["NO"]
        ]


        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Дом, вторичка"

        XCTAssertEqual(description, expectedDescription)
    }

    // 6
    func testBuyLot() {
        let query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["LOT"],
            QueryFilterParameters.objectType: ["OFFER"],
            QueryFilterParameters.priceMin: ["10"],
            QueryFilterParameters.priceMax: ["100"],
            QueryFilterParameters.priceType: ["PER_ARE"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Участок, от 10 до 100\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за сотку"

        XCTAssertEqual(description, expectedDescription)
    }

    // 7
    func testBuyCommercial() {
        var query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["COMMERCIAL"]
        ]

        for commercialType in self.commercialTypes {
            query[QueryFilterParameters.commercialType] = [commercialType]

            let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
            let expectedDescription = "\(commercialTypeLocalizationRule[commercialType] ?? "")".yre_capitalizingFirstLetter()

            XCTAssertEqual(description, expectedDescription)
        }
    }

    // 8
    func testBuyGarage() {
        var query = [
            QueryFilterParameters.type: ["SELL"],
            QueryFilterParameters.category: ["GARAGE"]
        ]

        for garageType in self.garageTypes {
            query[QueryFilterParameters.garageType] = [garageType]

            let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
            let expectedDescription = "\(garageTypeLocalizationRule[garageType] ?? "")".yre_capitalizingFirstLetter()

            XCTAssertEqual(description, expectedDescription)
        }
    }

    // 9
    func testShortRentApartment() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["APARTMENT"],
            QueryFilterParameters.rentTime: ["SHORT"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["30000"],
            QueryFilterParameters.roomsTotal: ["3", "4"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Квартира, 3, 4 комн., от 100 до 30 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в сутки"

        XCTAssertEqual(description, expectedDescription)
    }

    // 10
    func testLongRentApartment() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["APARTMENT"],
            QueryFilterParameters.rentTime: ["LARGE"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["30000"],
            QueryFilterParameters.roomsTotal: ["3", "4"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Квартира, 3, 4 комн., от 100 до 30 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в месяц"

        XCTAssertEqual(description, expectedDescription)
    }

    // 11
    func testShortRentRoom() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["ROOMS"],
            QueryFilterParameters.rentTime: ["SHORT"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["30000"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Комната, от 100 до 30 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в сутки"

        XCTAssertEqual(description, expectedDescription)
    }

    // 12
    func testLongRentRoom() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["ROOMS"],
            QueryFilterParameters.rentTime: ["LARGE"],
            QueryFilterParameters.priceMin: ["100"],
            QueryFilterParameters.priceMax: ["30000"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Комната, от 100 до 30 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в месяц"

        XCTAssertEqual(description, expectedDescription)
    }

    // 13
    func testShortRentHouse() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["HOUSE"],
            QueryFilterParameters.rentTime: ["SHORT"],
            QueryFilterParameters.priceMax: ["30000"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Дом, до 30 тыс.\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в сутки"

        XCTAssertEqual(description, expectedDescription)
    }

    // 14
    func testRentCommercial() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["COMMERCIAL"],
            QueryFilterParameters.priceMax: ["100"],
            QueryFilterParameters.pricingPeriod: ["PER_YEAR"],
            QueryFilterParameters.priceType: ["PER_METER"],
            QueryFilterParameters.areaMax: ["100"],
            QueryFilterParameters.commercialType: ["OFFICE", "BUSINESS"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Офис, готовый бизнес, до 100\(SpecialSymbol.nbsp)м², до 100\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за м²\(SpecialSymbol.nbsp)в год"

        XCTAssertEqual(description, expectedDescription)
    }

    // 15
    func testRentGarage() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["GARAGE"],
            QueryFilterParameters.priceMin: ["10"],
            QueryFilterParameters.priceMax: ["30"],
            QueryFilterParameters.garageType: ["BOX"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Бокс, от 10 до 30\(SpecialSymbol.nbsp)₽"

        XCTAssertEqual(description, expectedDescription)
    }

    // 16
    func testShortRentRoomWithoutPrice() {
        let query = [
            QueryFilterParameters.type: ["RENT"],
            QueryFilterParameters.category: ["ROOMS"],
            QueryFilterParameters.rentTime: ["SHORT"]
        ]

        let description = SavedSearchWidgetFiltersFormatter().formatQuery(for: query)
        let expectedDescription = "Комната, посуточно"

        XCTAssertEqual(description, expectedDescription)
    }

    // 17
    func testRoomsFormatter() {
        let rooms = ["1", "2", "3", "PLUS_4"]

        let formattedRoomNumber = RoomsTotalFormatter.formatRoomsTotal(roomsTotal: rooms)
        let expectedText = "1–3, 4+ комн."

        XCTAssertEqual(formattedRoomNumber, expectedText)
    }

    private let villageOfferTypeLocalizationRule = ["COTTAGE": "дом", "TOWNHOUSE": "таунхаус", "LAND": "участок"]
    private let villageOfferTypes = ["COTTAGE", "TOWNHOUSE", "LAND"]
    private let commercialTypeLocalizationRule = [
        "LAND": "земельный участок",
        "OFFICE": "офис",
        "RETAIL": "торговое помещение",
        "FREE_PURPOSE": "помещение своб. назначения",
        "WAREHOUSE": "склад",
        "MANUFACTURING": "производ. помещение",
        "PUBLIC_CATERING": "общепит",
        "AUTO_REPAIR": "автосервис",
        "HOTEL": "гостиница",
        "BUSINESS": "готовый бизнес",
        "LEGAL_ADDRESS": "юридический адрес"
    ]
    private let commercialTypes = [
        "LAND",
        "OFFICE",
        "RETAIL",
        "FREE_PURPOSE",
        "WAREHOUSE",
        "MANUFACTURING",
        "PUBLIC_CATERING",
        "AUTO_REPAIR",
        "HOTEL",
        "BUSINESS",
        "LEGAL_ADDRESS"
    ]

    private let garageTypeLocalizationRule = [
        "BOX": "бокс",
        "GARAGE": "гараж",
        "PARKING_PLACE": "машиноместо"
    ]

    private let garageTypes = ["BOX", "GARAGE", "PARKING_PLACE"]
}
