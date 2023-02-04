//
//  OfferSnippetTests.swift
//  Unit Tests
//
//  Created by Pavel Zhuravlev on 28.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
import YREModelHelpers
import YRESnippets

/// This class is for both `OfferSnippetView` and `OfferSnippetViewModelGenerator`, so it has such name.
final class OfferSnippetTests: XCTestCase {}

// MARK: - Utils

extension OfferSnippetTests {
    static func makeView(
        with viewModel: OfferSnippetViewModel
    ) -> OfferSnippetView {
        let view = OfferSnippetView()
        view.viewModel = viewModel
        view.frame = Self.makeFrame(forConfiguredView: view)
        return view
    }

    static func makeFrame(forConfiguredView view: OfferSnippetView) -> CGRect {
        guard let viewModel = view.viewModel else {
            XCTFail("View is not configured")
            return .zero
        }
        let width = UIScreen.main.bounds.width
        let height = OfferSnippetView.height(
            width: width,
            layout: view.layout,
            layoutStyle: view.layoutStyle,
            viewModel: viewModel
        )
        let result = CGRect(
            origin: .zero,
            size: CGSize(width: width, height: height)
        )
        return result
    }

    static func makeViewModel(with snippet: YREOfferSnippet) -> OfferSnippetViewModel? {
        let provider = YREAbstractOfferInfoProvider(
            offer: snippet,
            inFavorites: false,
            inCallHistory: false,
            isViewed: false,
            requestingPhones: false
        )
        guard let snippetProvider = provider.asOfferSnippetProvider() else {
            XCTFail("Unable to obtain offer snippet provider")
            return nil
        }
        let viewModel = OfferSnippetViewModelGenerator.makeViewModel(
            viewMode: .list,
            offerSnippetInfoProvider: snippetProvider,
            restrictedActions: [],
            selectedImageIndex: 0,
            enforceWholePrice: false,
            hideVASIcons: false
        )
        return viewModel
    }

    static func makeLocation(
        subjectFederationID: SubjectFederationID,
        address: String? = "Какой-то город, Какая-то улица, 66",
        streetAddress: String? = "Пятницкое шоссе, 2"
    ) -> YRELocation {
        let location = YRELocation(
            regionID: nil,
            geoID: nil,
            subjectFederationId: NSNumber(value: subjectFederationID.rawValue),
            subjectFederationRgid: nil,
            subjectFederationName: nil,
            address: address,
            streetAddress: streetAddress,
            geocoderAddress: nil,
            point: nil,
            allHeatmapPoints: nil,
            expectedMetros: nil,
            ponds: nil,
            parks: nil,
            schools: nil,
            metroList: nil
        )
        return location
    }

    static func makeBuilding(
        siteID: String? = nil,
        siteDisplayName: String? = nil
    ) -> YREBuilding {
        let building = YREBuilding(
            builtYear: nil,
            buildingSeries: nil,
            builtQuarter: nil,
            buildingState: ConstantParamBuildingState.unknown,
            siteId: siteID,
            building: ConstantParamBuildingConstructionType.unknown,
            buildingEpoch: BuildingEpoch.unknown,
            parkingType: .unknown,
            heatingType: .unknown,
            officeClass: .unknown,
            hasParking: .paramBoolUnknown,
            hasGuestParking: .paramBoolUnknown,
            hasLift: .paramBoolUnknown,
            hasRubbishChute: .paramBoolUnknown,
            hasPassBy: .paramBoolUnknown,
            hasAlarm: .paramBoolUnknown,
            forCityRenovation: .paramBoolUnknown,
            guarded: .paramBoolUnknown,
            security: .paramBoolUnknown,
            hasAccessControlSystem: .paramBoolUnknown,
            twentyFourSeven: .paramBoolUnknown,
            hasEatingFacilities: .paramBoolUnknown,
            hasCCTV: .paramBoolUnknown,
            siteDisplayName: siteDisplayName,
            houseReadableName: nil,
            flatsCount: nil,
            porchCount: nil,
            reconstructionYear: nil,
            developerIds: nil,
            hasDeveloperChat: .paramBoolUnknown
        )
        return building
    }

    static func makeSellOfferSnippet(
        category: kYREOfferCategory = .apartment,
        location: YRELocation,
        metro: YREMetro? = nil,
        building: YREBuilding? = nil,
        vas: OfferVAS? = nil,
        price: YREPrice? = nil,
        offerPriceInfo: YREOfferPriceInfo? = nil,
        author: YREAuthor? = nil,
        isNewFlatSale: Bool? = nil,
        hasEGRNReport: Bool = false,
        isFullTrustedOwner: Bool = false,
        isOutdated: Bool = false,
        virtualTours: [VirtualTour]? = nil
    ) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            type: .sell,
            category: category,
            location: location,
            metro: metro,
            building: building,
            vas: vas,
            price: price,
            offerPriceInfo: offerPriceInfo,
            author: author,
            isNewFlatSale: isNewFlatSale,
            hasEGRNReport: hasEGRNReport,
            isFullTrustedOwner: isFullTrustedOwner,
            isOutdated: isOutdated,
            virtualTours: virtualTours
        )
    }

    static func makeRentOfferSnippet(
        location: YRELocation,
        metro: YREMetro? = nil,
        building: YREBuilding? = nil,
        vas: OfferVAS? = nil,
        price: YREPrice? = nil,
        offerPriceInfo: YREOfferPriceInfo? = nil,
        author: YREAuthor? = nil,
        isYandexRent: Bool = false,
        isFullTrustedOwner: Bool = false,
        isOutdated: Bool = false,
        virtualTours: [VirtualTour]? = nil
    ) -> YREOfferSnippet {
        return Self.makeOfferSnippet(
            type: .rent,
            location: location,
            metro: metro,
            building: building,
            vas: vas,
            price: price,
            offerPriceInfo: offerPriceInfo,
            author: author,
            isYandexRent: isYandexRent,
            isFullTrustedOwner: isFullTrustedOwner,
            isOutdated: isOutdated,
            virtualTours: virtualTours
        )
    }
    
    private static func makeOfferSnippet(
        type: kYREOfferType,
        category: kYREOfferCategory = .apartment,
        location: YRELocation,
        metro: YREMetro? = nil,
        building: YREBuilding? = nil,
        vas: OfferVAS? = nil,
        price: YREPrice? = nil,
        offerPriceInfo: YREOfferPriceInfo? = nil,
        author: YREAuthor? = nil,
        isNewFlatSale: Bool? = nil,
        isYandexRent: Bool = false,
        hasEGRNReport: Bool = false,
        isFullTrustedOwner: Bool = false,
        isOutdated: Bool = false,
        virtualTours: [VirtualTour]? = nil
    ) -> YREOfferSnippet {
        return YREOfferSnippet(
            identifier: "1234",
            type: type,
            category: category,
            partnerId: nil,
            internal: .paramBoolFalse,
            creationDate: nil,
            update: nil,
            newFlatSale: ConstantParamBool.make(bool: isNewFlatSale),
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
            metro: metro,
            price: price,
            offerPriceInfo: offerPriceInfo,
            vas: vas,
            roomsOffered: 1,
            roomsTotal: 1,
            area: YREArea(unit: .m2, value: 34),
            livingSpace: YREArea(unit: .m2, value: 14),
            floorsTotal: 1,
            floorsOffered: [1],
            lotArea: nil,
            lotType: .unknown,
            housePart: .paramBoolFalse,
            houseType: .unknown,
            studio: .paramBoolFalse,
            commissioningDate: nil,
            isFreeReportAvailable: ConstantParamBool.make(bool: hasEGRNReport),
            isPurchasingReportAvailable: .paramBoolFalse,
            building: building,
            garage: nil,
            author: author,
            isFullTrustedOwner: ConstantParamBool.make(bool: isFullTrustedOwner),
            salesDepartments: nil,
            commercialDescription: nil,
            villageInfo: nil,
            siteInfo: nil,
            isOutdated: isOutdated,
            viewsCount: 0,
            uid: nil,
            share: nil,
            queryContext: nil,
            apartment: nil,
            chargeForCallsType: .byCampaign,
            yandexRent: ConstantParamBool.make(bool: isYandexRent),
            userNote: nil,
            virtualTours: virtualTours,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}
