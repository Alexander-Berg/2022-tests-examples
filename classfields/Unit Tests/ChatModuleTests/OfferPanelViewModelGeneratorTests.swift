//
//  OfferPanelViewModelGeneratorTests.swift
//  Pods
//
//  Created by Pavel Zhuravlev on 27.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
@testable import YREChatModule
@testable import YREDesignKit

final class OfferPanelViewModelGeneratorTests: XCTestCase { }

// MARK: - Offer

extension OfferPanelViewModelGeneratorTests {
    func testUserCharWithOfferID() {
        let offerID = "1234"
        let model = UserChatSubject(
            avatar: nil,
            offerData: UserChatSubject.OfferData.offerID(offerID),
            isOfferRemoved: false
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        XCTAssertEqual(viewModel, .outdatedEmptyOffer, "Expected outdated offer view model")
    }

    func testUserCharWithCommonOfferSnippet() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let offerID = "1234"
        let address = "Москва, Чертановская улица, 9"

        let priceValue = 3_600_000
        let priceString = "3 600 000 ₽"

        let rooms = 2
        let area = 62.2
        let description = "2 комн., 62,2 м²"

        let snippet = Self.makeOfferSnippet(
            id: offerID,
            address: address,
            priceValue: priceValue,
            rooms: rooms,
            areaValue: area
        )
        let model = UserChatSubject(
            avatar: avatarURL,
            offerData: UserChatSubject.OfferData.offerSnippet(snippet),
            isOfferRemoved: false
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected offer image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), priceString, "Unexpected price")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), description, "Unexpected description")
        XCTAssertEqual(contentViewModel.secondaryDescription?.yre_removeNBSPs(), address, "Unexpected address")
    }

    // MARK: Private

    private static func makeOfferSnippet(
        id: String,
        address: String,
        priceValue: Int,
        rooms: Int,
        areaValue: Double
    ) -> YREOfferSnippet {
        let location = Self.makeLocation(address: address)
        let price = YREPrice(
            currency: .RUB,
            value: NSNumber(value: priceValue),
            unit: .perOffer,
            period: .wholeLife
        )
        let area = YREArea(
            unit: .m2,
            value: areaValue
        )
        let offer = YREOfferSnippet(
            identifier: id,
            type: .sell,
            category: .apartment,
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
            location: location,
            metro: nil,
            price: price,
            offerPriceInfo: nil,
            vas: nil,
            roomsOffered: rooms,
            roomsTotal: rooms,
            area: area,
            livingSpace: nil,
            floorsTotal: 0,
            floorsOffered: [],
            lotArea: nil,
            lotType: .unknown,
            housePart: .paramBoolUnknown,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commissioningDate: nil,
            isFreeReportAvailable: .paramBoolUnknown,
            isPurchasingReportAvailable: .paramBoolUnknown,
            building: nil,
            garage: nil,
            author: nil,
            isFullTrustedOwner: .paramBoolUnknown,
            salesDepartments: nil,
            commercialDescription: nil,
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
        return offer
    }
}

// MARK: - Site

extension OfferPanelViewModelGeneratorTests {
    func testDevChatWithSiteID() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteID(siteID)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        XCTAssertNil(viewModel, "Expected empty view model")
    }

    func testDevChatWithRemovedSiteOffer() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let offerID = "4321"
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteOfferID(siteID: siteID, site: nil, offerID: offerID, isOfferRemoved: true)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        XCTAssertEqual(viewModel, .outdatedEmptyOffer, "Expected outdated view model")
    }

    func testDevChatWithSiteOfferIDWithoutSiteSnippet() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let offerID = "4321"
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteOfferID(siteID: siteID, site: nil, offerID: offerID, isOfferRemoved: false)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        XCTAssertNil(viewModel, "Expected empty view model")
    }

    func testDevChatWithCommonSiteOfferID() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let offerID = "4321"
        let address = "Москва, Чертановская улица, 9"
        let siteName = "ЖК Два Стула"

        let priceRange = YREPriceRange(
            from: 3_000_000,
            to: 4_000_000,
            average: nil,
            currency: .RUB,
            unit: .perOffer,
            period: .wholeLife
        )
        let priceString = "3 — 4 млн ₽"

        let siteSnippet = Self.makeSiteSnippet(
            siteID: siteID,
            siteName: siteName,
            address: address,
            priceRange: priceRange
        )
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteOfferID(siteID: siteID, site: siteSnippet, offerID: offerID, isOfferRemoved: false)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), siteName, "Unexpected site name")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), address, "Unexpected address")
        XCTAssertEqual(contentViewModel.secondaryDescription?.yre_removeNBSPs(), priceString, "Unexpected price")
    }

    func testDevChatWithCommonSiteSnippet() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let address = "Москва, Чертановская улица, 9"
        let siteName = "ЖК Два Стула"

        let priceRange = YREPriceRange(
            from: 3_000_000,
            to: 4_000_000,
            average: nil,
            currency: .RUB,
            unit: .perOffer,
            period: .wholeLife
        )
        let priceString = "3 — 4 млн ₽"

        let siteSnippet = Self.makeSiteSnippet(
            siteID: siteID,
            siteName: siteName,
            address: address,
            priceRange: priceRange
        )
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteSnippet(siteSnippet)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), siteName, "Unexpected site name")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), address, "Unexpected address")
        XCTAssertEqual(contentViewModel.secondaryDescription?.yre_removeNBSPs(), priceString, "Unexpected price")
    }

    func testDevChatWithCommonSiteSnippetHavingEmptyFields() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let address = "Москва, Чертановская улица, 9"

        let siteSnippet = Self.makeSiteSnippet(
            siteID: siteID,
            siteName: nil,
            address: address,
            priceRange: nil
        )
        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteSnippet(siteSnippet)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), "ЖК", "Unexpected site name")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), address, "Unexpected address")
        XCTAssertNil(contentViewModel.secondaryDescription, "Unexpected price")
    }

    func testDevChatWithSiteOfferWithoutSiteSnippet() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let offerID = "4321"
        let address = "Москва, Чертановская улица, 9"

        let priceValue = 3_600_000
        let priceString = "3 600 000 ₽"

        let rooms = 2
        let area = 62.2
        let description = "2 комн., 62,2 м²"

        let offerSnippet = Self.makeOfferSnippet(
            id: offerID,
            address: address,
            priceValue: priceValue,
            rooms: rooms,
            areaValue: area
        )

        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteOffer(siteID: siteID, site: nil, offer: offerSnippet)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected offer image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), priceString, "Unexpected price")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), description, "Unexpected description")
        XCTAssertEqual(contentViewModel.secondaryDescription?.yre_removeNBSPs(), address, "Unexpected address")
    }

    func testDevChatWithSiteOfferHavingSiteSnippet() {
        let avatarURL = URL(string: "http://avatars.mds.yandex.net/some-photo")
        let siteID = "1234"
        let offerID = "4321"
        let address = "Москва, Чертановская улица, 9"
        let siteName = "ЖК Два Стула"

        let priceValue = 3_600_000
        let priceString = "3 600 000 ₽"

        let rooms = 2
        let area = 62.2
        let description = "2 комн., 62,2 м²"

        let offerSnippet = Self.makeOfferSnippet(
            id: offerID,
            address: address,
            priceValue: priceValue,
            rooms: rooms,
            areaValue: area
        )
        let siteSnippet = Self.makeSiteSnippet(
            siteID: siteID,
            siteName: siteName,
            address: address,
            priceRange: nil
        )

        let model = DevChatSubject(
            avatar: avatarURL,
            siteData: .siteOffer(siteID: siteID, site: siteSnippet, offer: offerSnippet)
        )
        let generator = OfferPanelViewModelGenerator()
        let viewModel = generator.makeOfferPanelViewModel(from: model)

        guard case let .content(contentViewModel) = viewModel else {
            XCTFail("Expected content offer view model")
            return
        }
        XCTAssertEqual(contentViewModel.offerImage, avatarURL, "Unexpected offer image")
        XCTAssertEqual(contentViewModel.title.yre_removeNBSPs(), priceString, "Unexpected price")
        XCTAssertEqual(contentViewModel.primaryDescription.yre_removeNBSPs(), description, "Unexpected primary description")
        XCTAssertEqual(contentViewModel.secondaryDescription?.yre_removeNBSPs(), siteName, "Unexpected description")
    }

    // MARK: Private

    private static func makeSiteSnippet(
        siteID: String,
        siteName: String?,
        address: String,
        priceRange: YREPriceRange?
    ) -> YRESiteSnippet {
        let priceInfo = YRESitePriceInfo(
            priceRange: priceRange,
            priceRangePerMeter: nil,
            priceRatioToMarket: nil,
            totalOffers: nil,
            rooms: nil,
            priceStatistics: nil
        )
        let location = Self.makeLocation(
            address: address
        )
        return YRESiteSnippet(
            identifier: siteID,
            name: siteName,
            shortName: nil,
            large1242ImageURLs: nil,
            largeImageURLs: nil,
            middleImageURLs: nil,
            fullImageURLs: nil,
            location: location,
            metro: nil,
            filterStatistics: nil,
            resaleFilterStatistics: nil,
            priceInfo: priceInfo,
            siteDescription: nil,
            developers: nil,
            salesDepartments: nil,
            deliveryDates: nil,
            summarySpecialProposals: nil,
            salesClosed: .paramBoolUnknown,
            flatStatus: .unknown,
            isOutdated: false,
            queryContext: nil,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}

// MARK: - Utils

extension OfferPanelViewModelGeneratorTests {
    private static func makeLocation(
        address: String
    ) -> YRELocation {
        return YRELocation(
            regionID: nil,
            geoID: nil,
            subjectFederationId: nil,
            subjectFederationRgid: nil,
            subjectFederationName: nil,
            address: address,
            streetAddress: nil,
            geocoderAddress: nil,
            point: nil,
            allHeatmapPoints: nil,
            expectedMetros: nil,
            ponds: nil,
            parks: nil,
            schools: nil,
            metroList: nil
        )
    }
}
