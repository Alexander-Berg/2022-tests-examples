//
//  VillageSnippetTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 11.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRECoreUI
import YREModel
import YREModelObjc
import YREModelHelpers
@testable import YRESnippets

/// This class is for both `VillageSnippetView` and `VillageSnippetViewModelGenerator`, so it has such name.
final class VillageSnippetTests: XCTestCase {
    func testNoPhoto() {
        let snippet = Self.makeVillageSnippet(
            photos: []
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testLongVillageName() {
        let snippet = Self.makeVillageSnippet(
            name: "Очень длинное название, которое явно не влезет в обычный сниппет"
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testLongDeveloperName() {
        let developer = Self.makeVillageDeveloper(
            name: "Очень длинное название, которое явно не влезет в обычный сниппет"
        )
        let snippet = Self.makeVillageSnippet(
            developers: [developer]
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testMultipleDevelopers() {
        let developer1 = Self.makeVillageDeveloper(
            name: "Застройщик 1"
        )
        let developer2 = Self.makeVillageDeveloper(
            name: "Застройщик 2"
        )
        let developer3 = Self.makeVillageDeveloper(
            name: "Застройщик 3"
        )
        let developer4 = Self.makeVillageDeveloper(
            name: "Застройщик 4"
        )
        let snippet = Self.makeVillageSnippet(
            developers: [developer1, developer2, developer3, developer4]
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testLongAddress() {
        let location = Self.makeVillageLocation(
            address: "Очень длинный адрес, который явно не влезет в обычный сниппет"
        )
        let snippet = Self.makeVillageSnippet(
            location: location
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testCommonPrice() {
        let priceInfo = Self.makeVillagePriceInfo(
            lowerPriceValue: 100000,
            upperPriceValue: 200000,
            averagePriceValue: 150000
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testLargePrice() {
        let priceInfo = Self.makeVillagePriceInfo(
            lowerPriceValue: 100_000_000_000_000_000,
            upperPriceValue: 200_000_000_000_000_000,
            averagePriceValue: 150_000_000_000_000_000
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testNoPrice() {
        let snippet = Self.makeVillageSnippet(
            priceInfo: nil
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }
}

// MARK: - Village class

extension VillageSnippetTests {
    func testVillageClassEconom() {
        let features = Self.makeVillageFeatures(
            villageClass: .economy
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageClassComfort() {
        let features = Self.makeVillageFeatures(
            villageClass: .comfort
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageClassComfortPlus() {
        let features = Self.makeVillageFeatures(
            villageClass: .comfortPlus
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageClassBusiness() {
        let features = Self.makeVillageFeatures(
            villageClass: .business
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageClassElite() {
        let features = Self.makeVillageFeatures(
            villageClass: .elite
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageClassUnknown() {
        let features = Self.makeVillageFeatures(
            villageClass: .unknown
        )
        let snippet = Self.makeVillageSnippet(
            features: features
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }
}

// MARK: - Offer type

extension VillageSnippetTests {
    func testVillageOfferTypeLand() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.land]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageOfferTypeTownhouse() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.townhouse]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageOfferTypeCottage() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.cottage]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageOfferTypeLandAndTownhouse() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.land, .townhouse]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageOfferTypeLandTownhouseAndCottage() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.land, .townhouse, .cottage]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }

    func testVillageOfferTypeUnknown() {
        let priceInfo = Self.makeVillagePriceInfo(
            offerTypes: [.unknown]
        )
        let snippet = Self.makeVillageSnippet(
            priceInfo: priceInfo
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }
}

// MARK: - Status

extension VillageSnippetTests {
    /// Other options are checked by `DeliveryInfoFormatterTests`
    func testStatusSuspendedAndHandOver() {
        let deliveryDate1 = VillageDeliveryDate(
            name: "1 очередь",
            index: 1,
            status: .handOver,
            year: 2016,
            quarter: 4,
            isFinished: true
        )
        let deliveryDate2 = VillageDeliveryDate(
            name: "2 очередь",
            index: 2,
            status: .suspended,
            year: 2100,
            quarter: 5,
            isFinished: false
        )
        let snippet = Self.makeVillageSnippet(
            deliveryDates: [deliveryDate1, deliveryDate2]
        )
        let viewModel = Self.makeViewModel(with: snippet)
        let view = Self.makeView(with: viewModel)
        self.assertSnapshot(view)
    }
}

// MARK: - Utils

extension VillageSnippetTests {
    private static func makeView(
        with viewModel: VillageSnippetViewModel?
    ) -> VillageSnippetView {
        let view = VillageSnippetView()

        guard let viewModel = viewModel else {
            XCTFail("ViewModel is not provided")
            return view
        }

        view.viewModel = viewModel

        view.frame = Self.frame(by: { width in
            let height = VillageSnippetView.height(
                width: width,
                layout: view.layout,
                layoutStyle: view.layoutStyle,
                viewModel: viewModel
            )
            return height
        })
        return view
    }

    private static func makeViewModel(with snippet: VillageSnippet) -> VillageSnippetViewModel? {
        let provider = YREAbstractOfferInfoProvider(
            offer: snippet,
            inFavorites: false,
            inCallHistory: false,
            isViewed: false,
            requestingPhones: false
        )
        guard let snippetProvider = provider.asVillageSnippetProvider() else {
            XCTFail("Unable to obtain offer snippet provider")
            return nil
        }
        return VillageSnippetViewModelGenerator.makeViewModel(
            viewMode: .list,
            villageSnippetInfoProvider: snippetProvider,
            selectedImageIndex: 0
        )
    }

    private static func makeVillageSnippet(
        name: String = "Test",
        developers: [VillageDeveloper]? = nil,
        location: VillageLocation? = nil,
        features: VillageFeatures? = nil,
        transactionTerms: VillageTransactionTerms? = nil,
        communications: VillageCommunications? = nil,
        photos: [VillagePhoto]? = nil,
        deliveryDates: [VillageDeliveryDate]? = nil,
        priceInfo: VillagePriceInfo? = nil
    ) -> VillageSnippet {
        return VillageSnippet(
            identifier: "1234",
            villageType: .common,
            name: name,
            fullName: nil,
            urlString: nil,
            descriptionText: nil,
            developers: developers,
            location: location ?? Self.makeVillageLocation(),
            features: features ?? Self.makeVillageFeatures(),
            transactionTerms: transactionTerms ?? Self.makeVillageTransactionTerms(),
            communications: communications ?? Self.makeVillageCommunications(),
            panoramaUrls: nil,
            videoUrls: nil,
            villageOptions: nil,
            constructionStates: nil,
            photos: photos,
            deliveryDates: deliveryDates,
            salesDepartments: nil,
            offerStats: nil,
            mainPhoto: nil,
            shareUrl: nil,
            priceInfo: priceInfo,
            queryContext: nil,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}

extension VillageSnippetTests {
    private static func makeVillageDeveloper(
        name: String = "Какой-то застройщик"
    ) -> VillageDeveloper {
        let statistics = VillageDeveloperStatistics(
            allCount: nil,
            salesOpenedCount: nil,
            finishedCount: nil,
            unfinishedCount: nil,
            suspendedCount: nil
        )
        return VillageDeveloper(
            identifier: "111",
            name: name,
            legalName: nil,
            urlString: nil,
            logoUrlString: nil,
            statistics: statistics,
            phones: nil,
            address: nil
        )
    }

    private static func makeVillagePriceInfo(
        offerTypes: [VillageOfferType] = [.land],
        lowerPriceValue: UInt? = nil,
        upperPriceValue: UInt? = nil,
        averagePriceValue: UInt? = nil
    ) -> VillagePriceInfo {
        let priceFrom: YREPrice? = lowerPriceValue.map {
            YREPrice(
                currency: .RUB,
                value: NSNumber(value: $0),
                unit: .perOffer,
                period: .wholeLife
            )
        }
        let priceTo: YREPrice? = upperPriceValue.map {
            YREPrice(
                currency: .RUB,
                value: NSNumber(value: $0),
                unit: .perOffer,
                period: .wholeLife
            )
        }
        let averagePrice: YREPrice? = averagePriceValue.map {
            YREPrice(
                currency: .RUB,
                value: NSNumber(value: $0),
                unit: .perOffer,
                period: .wholeLife
            )
        }
        let priceRange = YREPriceRange(
            from: priceFrom,
            to: priceTo,
            averagePrice: averagePrice
        )

        let boxedOfferTypes = offerTypes.map { NSNumber(value: $0.rawValue) }

        return VillagePriceInfo(
            offerTypes: boxedOfferTypes,
            price: priceRange
        )
    }

    private static func makeVillageFeatures(
        villageClass: VillageClass = .economy,
        landType: VillageLandType = .izhs
    ) -> VillageFeatures {
        return VillageFeatures(
            villageClass: villageClass,
            totalArea: nil,
            landType: landType,
            totalObjects: nil,
            soldObjects: nil,
            security: nil,
            meansOfCommunication: nil,
            infrastructure: nil
        )
    }

    private static func makeVillageLocation(
        address: String = "Пятницкое шоссе, 2"
    ) -> VillageLocation {
        return VillageLocation(
            centerPoint: .init(lat: 0, lon: 0),
            rgid: "123",
            geoId: NSNumber(value: 12345),
            geocoderAddress: address,
            addressParts: nil,
            polygon: nil,
            highways: nil,
            stations: nil,
            address: address,
            subjectFederationId: nil,
            subjectFederationRgid: nil
        )
    }

    private static func makeVillageTransactionTerms() -> VillageTransactionTerms {
        return VillageTransactionTerms(
            hasMortgage: .paramBoolUnknown,
            hasInstalments: .paramBoolUnknown,
            hasMaternityFunds: .paramBoolUnknown
        )
    }

    private static func makeVillageCommunications() -> VillageCommunications {
        return VillageCommunications(
            hasElectricity: .paramBoolUnknown,
            electricPower: nil,
            hasGas: .paramBoolUnknown,
            heating: .unknown,
            sewerage: .unknown,
            waterSupply: .unknown
        )
    }
}
