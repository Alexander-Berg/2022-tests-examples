//
//  OfferCardHighlightsTests.swift
//  Unit Tests
//
//  Created by Arkady Smirnov on 4/2/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import AsyncDisplayKit
import YREModel
import YREModelObjc
import YREDesignKit
@testable import YREOfferCardModule

final class OfferCardHighlightsTests: XCTestCase {
    func testNewSaleStudioLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: true, isStudio: true)
        self.compareLayout(for: offer)
    }

    func testSecondarySaleStudioLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: false, isStudio: true)
        self.compareLayout(for: offer)
    }

    // Apartment = квартира
    func testNewSaleApartmentLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: true, isApartments: false)
        self.compareLayout(for: offer)
    }

    // Apartment = квартира
    func testSecondarySaleApartmentLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: false, isApartments: false)
        self.compareLayout(for: offer)
    }

    // Apartments = квартира + апартаменты
    func testNewSaleApartmentsLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: true, isApartments: true)
        self.compareLayout(for: offer)
    }

    // Apartments = квартира + апартаменты
    func testSecondarySaleApartmentsLayout() {
        let offer = Self.makeOffer(category: .apartment, isNewSale: false, isApartments: true)
        self.compareLayout(for: offer)
    }

    func testRoomLayout() {
        let offer = Self.makeOffer(category: .room)
        self.compareLayout(for: offer)
    }

    func testHouseLayout() {
        let offer = Self.makeOffer(category: .house)
        self.compareLayout(for: offer)
    }

    func testLotLayout() {
        let offer = Self.makeOffer(category: .lot)
        self.compareLayout(for: offer)
    }

    func testGarageLayout() {
        let offer = Self.makeOffer(category: .garage)
        self.compareLayout(for: offer)
    }

    func testCommercialLayout() {
        let offer = Self.makeOffer(category: .commercial)
        self.compareLayout(for: offer)
    }

    private func compareLayout(for offer: YREOffer, function: String = #function) {
        let highlights = OfferCardHighlightsBarItemViewModelGenerator.makeViewModels(for: offer)
        let highlightsBar = HighlightsBar()
        highlightsBar.items = highlights

        let size = HighlightsBar.size(for: UIScreen.main.bounds.width, items: highlights)
        highlightsBar.frame = CGRect(origin: .zero, size: size)

        highlightsBar.backgroundColor = ColorScheme.Background.primary

        self.assertSnapshot(highlightsBar, function: function)
    }

    // swiftlint:disable:next function_body_length
    static private func makeOffer(category: kYREOfferCategory,
                                  isNewSale: Bool = false,
                                  isStudio: Bool = false,
                                  isApartments: Bool = false) -> YREOffer {
        let house = YREHouse(
            bathroomUnit: .unknown,
            windowView: .unknown,
            windowType: .unknown,
            balconyType: .unknown,
            entranceType: .unknown,
            studio: isStudio ? .paramBoolTrue : .paramBoolFalse,
            apartments: isApartments ? .paramBoolTrue : .paramBoolFalse,
            pmg: .paramBoolFalse,
            housePart: .paramBoolFalse,
            houseType: .whole,
            kitchen: .paramBoolFalse,
            pool: .paramBoolFalse,
            billiard: .paramBoolFalse,
            sauna: .paramBoolFalse,
            toilet: .unknown,
            shower: .unknown,
            electricCapacity: nil,
            phoneLinesCount: nil)

        let commercialDescription = YRECommercialDescription(
            buildingType: .detachedBuilding,
            types: [NSNumber(value: ConstantParamCommercialType.hotel.rawValue)],
            purposes: nil,
            warehousePurposes: nil,
            hasRailwayNearby: .paramBoolFalse,
            hasTruckEntrance: .paramBoolFalse,
            hasRamp: .paramBoolFalse,
            hasOfficeWarehouse: .paramBoolFalse,
            hasOpenArea: .paramBoolFalse,
            hasThreePLService: .paramBoolFalse,
            hasFreightElevator: .paramBoolFalse
        )

        let garage = Garage(
            cooperativeName: nil,
            type: .box,
            ownershipType: .unknown,
            hasAutomaticGates: .paramBoolFalse,
            hasInspectionPit: .paramBoolFalse,
            hasCarWash: .paramBoolFalse,
            hasAutoRepair: .paramBoolFalse,
            hasCellar: .paramBoolFalse
        )
        return YREOffer(
            identifier: "test",
            type: .sell,
            category: category,
            partnerId: nil,
            internal: .paramBoolFalse,
            creationDate: Date(),
            newFlatSale: isNewSale ? .paramBoolTrue : .paramBoolFalse,
            primarySale: .paramBoolUnknown,
            flatType: .unknown,
            urlString: nil,
            update: nil,
            roomsTotal: 2,
            roomsOffered: 1,
            floorsTotal: 12,
            floorsOffered: [2],
            author: nil,
            isFullTrustedOwner: .paramBoolFalse,
            trust: .yreConstantParamOfferTrustUnknown,
            viewsCount: 0,
            floorCovering: .unknown,
            area: YREArea(unit: .m2, value: 100),
            livingSpace: YREArea(unit: .m2, value: 100),
            kitchenSpace: YREArea(unit: .m2, value: 100),
            roomSpace: [YREArea(unit: .m2, value: 100)],
            large1242ImageURLs: nil,
            appLargeImageURLs: nil,
            minicardImageURLs: nil,
            middleImageURLs: nil,
            largeImageURLs: nil,
            fullImageURLs: nil,
            previewImages: nil,
            offerPlanImages: nil,
            floorPlanImages: nil,
            youtubeVideoReviewURL: nil,
            suspicious: .paramBoolFalse,
            active: .paramBoolFalse,
            hasAlarm: .paramBoolFalse,
            housePart: .paramBoolFalse,
            price: nil,
            offerPriceInfo: nil,
            location: nil,
            metro: nil,
            house: house,
            building: nil,
            garage: garage,
            lotArea: YREArea(unit: .are, value: 100),
            lotType: .garden,
            offerDescription: nil,
            commissioningDate: nil,
            ceilingHeight: nil,
            haggle: .paramBoolFalse,
            mortgage: .paramBoolFalse,
            rentPledge: .paramBoolFalse,
            electricityIncluded: .paramBoolFalse,
            cleaningIncluded: .paramBoolFalse,
            withKids: .paramBoolFalse,
            withPets: .paramBoolFalse,
            supportsOnlineView: .paramBoolFalse,
            zhkDisplayName: nil,
            apartment: nil,
            dealStatus: .primarySale,
            agentFee: nil,
            commission: nil,
            prepayment: nil,
            securityPayment: nil,
            taxationForm: .nds,
            isFreeReportAvailable: .paramBoolFalse,
            isPurchasingReportAvailable: .paramBoolFalse,
            paidExcerptsInfo: nil,
            generatedFromSnippet: false,
            heating: .paramBoolFalse,
            water: .paramBoolFalse,
            sewerage: .paramBoolFalse,
            electricity: .paramBoolFalse,
            gas: .paramBoolFalse,
            utilitiesIncluded: .paramBoolFalse,
            salesDepartments: nil,
            isOutdated: false,
            uid: nil,
            share: nil,
            enrichedFields: nil,
            history: nil,
            commercialDescription: commercialDescription,
            villageInfo: nil,
            siteInfo: nil,
            vas: nil,
            queryContext: nil,
            chargeForCallsType: .unknown,
            yandexRent: .paramBoolFalse,
            userNote: nil,
            virtualTours: nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}
