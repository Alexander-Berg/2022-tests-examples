//
//  ChatsOfferOptionsFormatterTests.swift
//  YREFormatters-Unit-Tests
//
//  Created by Leontyev Saveliy on 25.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREModel
import YREModelObjc
import enum Typograf.SpecialSymbol

final class ChatsOfferOptionsFormatterTests: XCTestCase {
    func testAppartmentOffer() {
        let testConfigs: [(roomsTotal: Int, area: YREArea?, studio: ConstantParamBool, result: String)] = [
            (0, nil, .paramBoolFalse, "Квартира"),
            (0, nil, .paramBoolTrue, "Студия"),
            (Consts.roomsTotal, nil, .paramBoolFalse, ResultStrings.roomsTotal),
            (Consts.roomsTotal, Consts.area, .paramBoolFalse, "\(ResultStrings.roomsTotal), \(ResultStrings.area)"),
        ]

        for config in testConfigs {
            let offer = Self.makeAppartmentOffer(roomsTotal: config.roomsTotal,
                                                 price: Consts.price,
                                                 area: config.area,
                                                 studio: config.studio)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testRoomOffer() {
        let testConfigs: [(roomsOffered: Int, roomsTotal: Int, livingSpace: YREArea?, result: String)] = [
            (0, 0, nil, "Комната"),
            (Consts.roomsOffered, 0, nil, "Комната"),
            (0, Consts.roomsTotal, nil, "Комната"),
            (Consts.roomsOffered, Consts.roomsTotal, nil, ResultStrings.roomsOffered),
            (Consts.roomsOffered, Consts.roomsTotal, Consts.area, "\(ResultStrings.roomsOffered), \(ResultStrings.area)"),
        ]

        for config in testConfigs {
            let offer = Self.makeRoomOffer(roomsOffered: config.roomsOffered,
                                           roomsTotal: config.roomsTotal,
                                           livingSpace: config.livingSpace)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testHouseOffer() {
        let testConfigs: [(houseType: HouseType, area: YREArea?, lotArea: YREArea?, result: String)] = [
            (.unknown, nil, nil, "Дом"),
            (.townHouse, nil, nil, ResultStrings.townhouse),
            (.townHouse, Consts.area, nil, "\(ResultStrings.townhouse), \(ResultStrings.area)"),
            (.townHouse, Consts.area, nil, "\(ResultStrings.townhouse), \(ResultStrings.area)"),
            (.townHouse, Consts.area, Consts.lotArea, "\(ResultStrings.townhouse), \(ResultStrings.area), \(ResultStrings.lotArea)"),
        ]

        for config in testConfigs {
            let offer = Self.makeHouseOffer(houseType: config.houseType, area: config.area, lotArea: config.lotArea)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testLotOffer() {
        let testConfigs: [(lotArea: YREArea?, result: String)] = [
            (nil, "Участок"),
            (Consts.lotArea, "Участок, \(ResultStrings.lotArea)"),
        ]

        for config in testConfigs {
            let offer = Self.makeLotOffer(lotArea: config.lotArea)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testCommercialOffer() {
        let testConfigs: [(commercialType: ConstantParamCommercialType, area: YREArea?, result: String)] = [
            (.unknown, nil, "Недвижимость"),
            (.hotel, nil, "Гостиница"),
            (.hotel, Consts.area, "Гостиница, \(ResultStrings.area)"),
        ]

        for config in testConfigs {
            let offer = Self.makeCommercialOffer(commercialType: config.commercialType, area: config.area)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testGarageOffer() {
        let testConfigs: [(garageType: ConstantParamGarageType, area: YREArea?, result: String)] = [
            (.unknown, nil, "Гараж"),
            (.box, nil, "Бокс"),
            (.box, Consts.area, "Бокс, \(ResultStrings.area)"),
        ]

        for config in testConfigs {
            let offer = Self.makeGarageOffer(garageType: config.garageType, area: config.area)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: false)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    func testIncludingPriceFormating() {
        let testConfigs: [(price: YREPrice?, result: String)] = [
            (nil, "Квартира"),
            (Consts.price, "Квартира, \(ResultStrings.price)"),
            (Consts.pricePerMonth, "Квартира, \(ResultStrings.pricePerMonth)"),
        ]

        for config in testConfigs {
            let offer = Self.makeAppartmentOffer(roomsTotal: 0,
                                                 price: config.price,
                                                 area: nil,
                                                 studio: .paramBoolUnknown)
            let offerOptions = self.formatter.optionsText(from: offer, includePrice: true)
            XCTAssertEqual(offerOptions, config.result)
        }
    }

    private enum  Consts {
        static let roomsOffered: Int = 2
        static let roomsTotal: Int = 3
        static let area = YREArea(unit: .m2, value: 123)
        static let lotArea = YREArea(unit: .hectare, value: 33)
        static let price = YREPrice(currency: .RUB, value: NSNumber(value: 123000), unit: .unknown, period: .unknown)
        static let pricePerMonth = YREPrice(currency: .RUB, value: NSNumber(value: 123000), unit: .unknown, period: .perMonth)
    }

    private enum ResultStrings {
        static let roomsTotal = "3\(nbsp)комн."
        static let area = "123\(nbsp)м²"
        static let lotArea = "33\(nbsp)га"
        static let roomsOffered = "2/3\(nbsp)комн."
        static let townhouse = "Таунхаус"
        static let price = "123\(nbsp)000\(nbsp)₽"
        static let pricePerMonth = "123\(nbsp)000\(nbsp)₽\(nbsp)в мес."

        static let nbsp = SpecialSymbol.nbsp
    }

    private let formatter = ChatsOfferOptionsFormatter()

    private static func makeAppartmentOffer(
        roomsTotal: Int,
        price: YREPrice?,
        area: YREArea?,
        studio: ConstantParamBool
    ) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            category: .apartment,
            roomsOffered: 0,
            roomsTotal: roomsTotal,
            price: price,
            area: area,
            lotArea: nil,
            livingSpace: nil,
            houseType: .unknown,
            studio: studio,
            commercialDescription: nil,
            garage: nil
        )
    }

    private static func makeRoomOffer(
        roomsOffered: Int,
        roomsTotal: Int,
        livingSpace: YREArea?
    ) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            category: .room,
            roomsOffered: roomsOffered,
            roomsTotal: roomsTotal,
            price: Consts.price,
            area: nil,
            lotArea: nil,
            livingSpace: livingSpace,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commercialDescription: nil,
            garage: nil
        )
    }

    private static func makeHouseOffer(houseType: HouseType, area: YREArea?, lotArea: YREArea?) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            category: .house,
            roomsOffered: 0,
            roomsTotal: 0,
            price: Consts.price,
            area: area,
            lotArea: lotArea,
            livingSpace: nil,
            houseType: houseType,
            studio: .paramBoolUnknown,
            commercialDescription: nil,
            garage: nil
        )
    }

    private static func makeLotOffer(lotArea: YREArea?) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            category: .lot,
            roomsOffered: 0,
            roomsTotal: 0,
            price: Consts.price,
            area: nil,
            lotArea: lotArea,
            livingSpace: nil,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commercialDescription: nil,
            garage: nil
        )
    }

    private static func makeCommercialOffer(
        commercialType: ConstantParamCommercialType,
        area: YREArea?
    ) -> YREOfferSnippet {
        let commercialDescription = Self.makeCommecrialDescription(commercialType: commercialType)

        return Self.makeOfferSnippet(
            category: .commercial,
            roomsOffered: 0,
            roomsTotal: 0,
            price: Consts.price,
            area: area,
            lotArea: nil,
            livingSpace: nil,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commercialDescription: commercialDescription,
            garage: nil
        )
    }

    private static func makeGarageOffer(garageType: ConstantParamGarageType, area: YREArea?) -> YREOfferSnippet {
        let garage = Self.makeGarage(type: garageType)
        return Self.makeOfferSnippet(
            category: .garage,
            roomsOffered: 0,
            roomsTotal: 0,
            price: Consts.price,
            area: area,
            lotArea: nil,
            livingSpace: nil,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commercialDescription: nil,
            garage: garage
        )
    }

    private static func makeGarage(type: ConstantParamGarageType) -> Garage {
        return Garage(
            cooperativeName: nil,
            type: type,
            ownershipType: .unknown,
            hasAutomaticGates: .paramBoolUnknown,
            hasInspectionPit: .paramBoolUnknown,
            hasCarWash: .paramBoolUnknown,
            hasAutoRepair: .paramBoolUnknown,
            hasCellar: .paramBoolUnknown
        )
    }

    private static func makeCommecrialDescription(commercialType: ConstantParamCommercialType) -> YRECommercialDescription {
        return YRECommercialDescription(
            buildingType: .unknown,
            types: [NSNumber(value: commercialType.rawValue)],
            purposes: nil,
            warehousePurposes: nil,
            hasRailwayNearby: .paramBoolUnknown,
            hasTruckEntrance: .paramBoolUnknown,
            hasRamp: .paramBoolUnknown,
            hasOfficeWarehouse: .paramBoolUnknown,
            hasOpenArea: .paramBoolUnknown,
            hasThreePLService: .paramBoolUnknown,
            hasFreightElevator: .paramBoolUnknown
        )
    }

    private static func makeOfferSnippet(
        category: kYREOfferCategory,
        roomsOffered: Int,
        roomsTotal: Int,
        price: YREPrice?,
        area: YREArea?,
        lotArea: YREArea?,
        livingSpace: YREArea?,
        houseType: HouseType,
        studio: ConstantParamBool,
        commercialDescription: YRECommercialDescription?,
        garage: Garage?
    ) -> YREOfferSnippet {
        return YREOfferSnippet(
            identifier: "",
            type: .unknown,
            category: category,
            partnerId: nil,
            internal: .paramBoolUnknown,
            creationDate: nil,
            update: nil,
            newFlatSale: .paramBoolUnknown,
            primarySale: .paramBoolUnknown,
            flatType: .unknown,
            large1242ImageURLs: nil,
            appLargeImageURLs: nil,
            largeImageURLs: nil,
            middleImageURLs: nil,
            miniImageURLs: nil,
            smallImageURLs: nil,
            fullImageURLs: nil,
            mainImageURLs: nil,
            previewImages: nil,
            offerPlanImages: nil,
            floorPlanImages: nil,
            youtubeVideoReviewURL: nil,
            location: nil,
            metro: nil,
            price: price,
            offerPriceInfo: nil,
            vas: nil,
            roomsOffered: roomsOffered,
            roomsTotal: roomsTotal,
            area: area,
            livingSpace: livingSpace,
            floorsTotal: 0,
            floorsOffered: [],
            lotArea: lotArea,
            lotType: .unknown,
            housePart: .paramBoolUnknown,
            houseType: houseType,
            studio: studio,
            commissioningDate: nil,
            isFreeReportAvailable: .paramBoolUnknown,
            isPurchasingReportAvailable: .paramBoolUnknown,
            building: nil,
            garage: garage,
            author: nil,
            isFullTrustedOwner: .paramBoolUnknown,
            salesDepartments: nil,
            commercialDescription: commercialDescription,
            villageInfo: nil,
            siteInfo: nil,
            isOutdated: false,
            viewsCount: 0,
            uid: nil,
            share: nil,
            queryContext: nil,
            apartment: nil,
            chargeForCallsType: .unknown,
            yandexRent: .paramBoolUnknown,
            userNote: nil,
            virtualTours: nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}
