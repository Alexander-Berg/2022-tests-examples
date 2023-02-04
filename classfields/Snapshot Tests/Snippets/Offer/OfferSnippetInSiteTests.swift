//
//  OfferSnippetInSiteTests.swift
//  Unit Tests
//
//  Created by Fedor Solovev on 23.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel
import YREModelObjc
@testable import YRESiteCardModule
@testable import YRESiteOfferListByPlanModule

final class OfferSnippetInSiteTests: XCTestCase {
    func testUnknownBuildingStateAndDecorationWithout3D() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .unknown, has3D: false)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testUnknownBuildingStateAndDecorationWithout3DWithImage() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .unknown, has3D: false)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: true)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testUnknownBuildingStateAndDecorationWith3DAndImage() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .unknown, has3D: true)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: true)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testBuiltBuildingState() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .built)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testUnfinishedBuildingState() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unfinished)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testHandOverBuildingState() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .handOver)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testSuspendedBuildingState() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .suspended)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testInProjectBuildingState() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .inProject)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testRoughDecoration() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .rough)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testCleanDecoration() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .clean)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testTurnkeyDecoration() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .turnkey)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testBuiltBuildingStateAndRoughDecoration() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .built, decoration: .rough)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testBuiltBuildingStateAndRoughDecorationWithImage() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .built, decoration: .rough)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: true)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testOverflowingWithImage() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .unknown, overflowing: true)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: true)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    func testOverflowingWithoutImage() {
        let offerSnippet = self.makeOfferSnippet(buildingState: .unknown, decoration: .unknown, overflowing: true)
        let viewModel = self.makeViewModel(offerSnippet, shouldDisplayImage: false)
        self.makeViewAndAssertSnapshot(viewModel)
    }

    private func makeViewModel(_ offerSnippet: YREOfferSnippet, shouldDisplayImage: Bool) -> SiteOfferByPlanViewModel {
        SiteOfferByPlanViewModelGenerator.makeViewModel(
            offerSnippet,
            isInFavorites: false,
            isViewed: false,
            shouldDisplayImage: shouldDisplayImage
        )
    }

    private func makeViewAndAssertSnapshot(_ viewModel: SiteOfferByPlanViewModel, function: String = #function) {
        let view = SiteOfferByPlanCell()
        view.frame = Self.frame(by: { width in
            SiteOfferByPlanCell.size(for: width, viewModel: viewModel).height
        })
        view.configure(viewModel: viewModel)

        self.assertSnapshot(view, function: function)
    }

    // swiftlint:disable:next function_body_length
    private func makeOfferSnippet(
        buildingState: ConstantParamBuildingState = .unknown,
        decoration: Decoration = .unknown,
        has3D: Bool = true,
        overflowing: Bool = false
    ) -> YREOfferSnippet {
        YREOfferSnippet(
            identifier: "1",
            type: .sell,
            category: .house,
            partnerId: nil,
            internal: .paramBoolUnknown,
            creationDate: nil,
            update: nil,
            newFlatSale: .paramBoolUnknown,
            primarySale: .paramBoolUnknown,
            flatType: .newFlat,
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
            price: nil,
            offerPriceInfo: YREOfferPriceInfo(
                price: .init(
                    currency: .RUB,
                    value: .init(value: overflowing ? 10_000_000_000_000_000 : 100),
                    unit: .perSquareMeter,
                    period: .wholeLife
                ),
                pricePerUnit: .init(
                    currency: .RUB,
                    value: .init(value: overflowing ? 10_000_000_000_000 : 50),
                    unit: .perSquareMeter,
                    period: .wholeLife
                ),
                wholePrice: .init(
                    currency: .RUB,
                    value: .init(value: overflowing ? 10_000_000_000_000 : 100),
                    unit: .perSquareMeter,
                    period: .wholeLife
                ),
                trend: .unchanged,
                previousPrice: nil,
                hasPriceHistory: nil
            ),
            vas: nil,
            roomsOffered: 2,
            roomsTotal: 2,
            area: .init(unit: .m2, value: 100),
            livingSpace: nil,
            floorsTotal: overflowing ? 200_000_000_000_000_000 : 20,
            floorsOffered: [.init(value: overflowing ? 50_000_000_000_000_000 : 5)],
            lotArea: nil,
            lotType: .unknown,
            housePart: .paramBoolUnknown,
            houseType: .unknown,
            studio: .paramBoolUnknown,
            commissioningDate: nil,
            isFreeReportAvailable: .paramBoolUnknown,
            isPurchasingReportAvailable: .paramBoolUnknown,
            building: YREBuilding(
                builtYear: 2022,
                buildingSeries: nil,
                builtQuarter: 1,
                buildingState: buildingState,
                siteId: nil,
                building: .unknown,
                buildingEpoch: .unknown,
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
                siteDisplayName: nil,
                houseReadableName: overflowing ? "ОЧЕНЬ БОЛЬШОЙ ЖК ОЧЕНЬ БОЛЬШОЙ ЖК ОЧЕНЬ БОЛЬШОЙ ЖК" : "ЖК ПКЖ",
                flatsCount: nil,
                porchCount: nil,
                reconstructionYear: nil,
                developerIds: nil,
                hasDeveloperChat: .paramBoolUnknown
            ),
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
            apartment: YREApartment(
                renovation: .unknown,
                quality: .unknown,
                decoration: decoration,
                phone: .paramBoolUnknown,
                internet: .paramBoolUnknown,
                selfSelectionTelecom: .paramBoolUnknown,
                roomFurniture: .paramBoolUnknown,
                kitchenFurniture: .paramBoolUnknown,
                buildInTech: .paramBoolUnknown,
                aircondiotion: .paramBoolUnknown,
                ventilation: .paramBoolUnknown,
                refrigerator: .paramBoolUnknown,
                noFurniture: .paramBoolUnknown,
                flatAlarm: .paramBoolUnknown,
                fireAlarm: .paramBoolUnknown,
                dishwasher: .paramBoolUnknown,
                washingMachine: .paramBoolUnknown,
                television: .paramBoolUnknown,
                addingPhoneOnRequest: .paramBoolUnknown,
                responsibleStorage: .paramBoolUnknown,
                flatPlanImage: nil
            ),
            chargeForCallsType: .unknown,
            yandexRent: .paramBoolUnknown,
            userNote: nil,
            virtualTours: has3D ? Self.makeVirtualTours() : nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }

    private static func makeVirtualTours() -> [VirtualTour] {
        // swiftlint:disable:next force_unwrapping
        let url = URL(string: "https://yandex.ru")!
        let matterportTour = MatterportTour(url: url, previewImage: nil)
        return [VirtualTour(tour: .matterport(matterportTour))]
    }
}
