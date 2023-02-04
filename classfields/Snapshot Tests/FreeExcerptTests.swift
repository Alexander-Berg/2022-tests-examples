//
//  FreeExcerptTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 12.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import AsyncDisplayKit
import YREModel
import YREModelObjc
@testable import YREOfferCardModule
import enum Typograf.SpecialSymbol

final class FreeExcerptTests: XCTestCase {
    func testFullExcerptLayout() {
        let node = ExcerptBlockNode()
        node.viewModel = ExcerptBlockViewModelGenerator.makeFreeReport(
            report: self.report(area: Self.validArea, floor: "13", currentRightsCount: 1),
            offer: Self.offer,
            isPurchasingExcerptAvailable: false
        )
        self.assertSnapshot(node)
    }

    func testShortExcerptLayout() {
        let node = ExcerptBlockNode()
        node.viewModel = ExcerptBlockViewModelGenerator.makeShortExcerptReport(
            report: self.report(area: Self.validArea, floor: "13", currentRightsCount: 1),
            offer: Self.offer
        )
        self.assertSnapshot(node)
    }

    func testUnauthorizedShortExcerptLayout() {
        let node = ExcerptBlockNode()
        node.viewModel = ExcerptBlockViewModelGenerator.makePromo()
        self.assertSnapshot(node)
    }

    func testShortExcerptTitles() {
        let generator = ExcerptBlockViewModelGenerator.self
        var viewModel = generator.makeEmpty()
        let nbsp = SpecialSymbol.nbsp

        let validReportWithNoOwners = self.report(
            area: Self.validArea,
            floor: Self.validFloorString,
            currentRightsCount: 0
        )
        viewModel = generator.makeShortExcerptReport(report: validReportWithNoOwners, offer: Self.offer)
        self.checkShortViewModel(
            viewModel: viewModel,
            expectedOwnersIcon: .warning,
            expectedOwnersTitle: "Нет данных о\(nbsp)текущих собственниках",
            expectedDataIcon: .okey,
            expectedDataTitle: "Сведения о\(nbsp)квартире совпадают"
        )

        let nonValidAreaReportWithOneOwner = self.report(
            area: 999,
            floor: Self.validFloorString,
            currentRightsCount: 1
        )
        viewModel = generator.makeShortExcerptReport(report: nonValidAreaReportWithOneOwner, offer: Self.offer)
        self.checkShortViewModel(
            viewModel: viewModel,
            expectedOwnersIcon: .okey,
            expectedOwnersTitle: "Один текущий собственник",
            expectedDataIcon: .warning,
            expectedDataTitle: "Сведения о\(nbsp)квартире не\(nbsp)совпадают"
        )

        let nonValidFloorReportWithTwoOwners = self.report(
            area: Self.validArea,
            floor: "999",
            currentRightsCount: 2
        )
        viewModel = generator.makeShortExcerptReport(report: nonValidFloorReportWithTwoOwners, offer: Self.offer)
        self.checkShortViewModel(
            viewModel: viewModel,
            expectedOwnersIcon: .okey,
            expectedOwnersTitle: "Два текущих собственника",
            expectedDataIcon: .warning,
            expectedDataTitle: "Сведения о\(nbsp)квартире не\(nbsp)совпадают"
        )

        let nonValidReportWithTenOwners = self.report(
            area: 999,
            floor: "999",
            currentRightsCount: 10
        )
        viewModel = generator.makeShortExcerptReport(report: nonValidReportWithTenOwners, offer: Self.offer)
        self.checkShortViewModel(
            viewModel: viewModel,
            expectedOwnersIcon: .warning,
            expectedOwnersTitle: "Более четырёх текущих собственников",
            expectedDataIcon: .warning,
            expectedDataTitle: "Сведения о\(nbsp)квартире не\(nbsp)совпадают"
        )
    }

    func testFullExcerptOwnersSection() {
        let generator = ExcerptBlockViewModelGenerator.self
        var viewModel = generator.makeEmpty()
        let nbsp = SpecialSymbol.nbsp

        let validReportWithFourOwners = self.report(
            area: Self.validArea,
            floor: Self.validFloorString,
            currentRightsCount: 4
        )
        viewModel = generator.makeFreeReport(
            report: validReportWithFourOwners,
            offer: Self.offer,
            isPurchasingExcerptAvailable: true
        )
        self.checkFullViewModel(viewModel: viewModel, expectedCurrentOwnersCount: 4, expectedHiddenOwnersTitle: nil)

        let validReportWithFiveOwners = self.report(
            area: Self.validArea,
            floor: Self.validFloorString,
            currentRightsCount: 5
        )
        viewModel = generator.makeFreeReport(
            report: validReportWithFiveOwners,
            offer: Self.offer,
            isPurchasingExcerptAvailable: true
        )
        self.checkFullViewModel(viewModel: viewModel, expectedCurrentOwnersCount: 5, expectedHiddenOwnersTitle: nil)

        let validReportWithSixOwners = self.report(
            area: Self.validArea,
            floor: Self.validFloorString,
            currentRightsCount: 6
        )
        viewModel = generator.makeFreeReport(
            report: validReportWithSixOwners,
            offer: Self.offer,
            isPurchasingExcerptAvailable: true
        )
        self.checkFullViewModel(
            viewModel: viewModel,
            expectedCurrentOwnersCount: 4,
            expectedHiddenOwnersTitle: "и\(nbsp)ещё\(nbsp)2 владельца"
        )
    }

    private func checkShortViewModel(
        viewModel: ExcerptBlockViewModel,
        expectedOwnersIcon: ShortExcerptItemView.ViewModel.IconStyle,
        expectedOwnersTitle: String,
        expectedDataIcon: ShortExcerptItemView.ViewModel.IconStyle,
        expectedDataTitle: String,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        switch viewModel.state {
            case .short(let itemViewModels):
                XCTAssertEqual(itemViewModels.count, 2, file: file, line: line)
                XCTAssertEqual(itemViewModels[0].iconStyle, expectedOwnersIcon, file: file, line: line)
                XCTAssertEqual(itemViewModels[0].title, expectedOwnersTitle, file: file, line: line)
                XCTAssertEqual(itemViewModels[1].iconStyle, expectedDataIcon, file: file, line: line)
                XCTAssertEqual(itemViewModels[1].title, expectedDataTitle, file: file, line: line)
            default:
                XCTFail("Unexpected state", file: file, line: line)
        }
    }

    private func checkFullViewModel(
        viewModel: ExcerptBlockViewModel,
        expectedCurrentOwnersCount: Int,
        expectedHiddenOwnersTitle: String?,
        file: StaticString = #file,
        line: UInt = #line
    ) {
        switch viewModel.state {
            case .fullFree(let fullFreeViewModel):
                XCTAssertEqual(
                    fullFreeViewModel.currentOwnersSection.owners.count,
                    expectedCurrentOwnersCount,
                    file: file,
                    line: line
                )
                XCTAssertEqual(
                    fullFreeViewModel.currentOwnersSection.hiddenOwnersTitle,
                    expectedHiddenOwnersTitle,
                    file: file,
                    line: line
                )
            default:
                XCTFail("Unexpected state", file: file, line: line)
        }
    }

    private func report(area: Double, floor: String?, currentRightsCount: Int) -> OfferExcerptReport {
        let creationDate = Date(timeIntervalSince1970: 1_610_545_100)
        let regDate = Date(timeIntervalSince1970: 0)

        let buildingInfo = OfferExcerptBuildingInfo(area: area, floorString: floor)

        var currentRights = [OfferExcerptRights]()
        for _ in 0..<currentRightsCount {
            let owner = OfferExcerptOwner(type: .natural, name: "Игорь")
            let registration = OfferExcerptRegistration(
                type: .ownership,
                regNumber: "213-321",
                regDate: regDate,
                endDate: nil,
                shareText: nil
            )
            let currentRight = OfferExcerptRights(owners: [owner], registration: registration)
            currentRights.append(currentRight)
        }

        let flatExcerptReport = FlatExcerptReport(
            cadastralCost: PriceValue(currency: .RUB, value: 100),
            buildingInfo: buildingInfo,
            currentRights: currentRights,
            previousRights: [],
            encumbrancesCount: 0
        )
        let report = OfferExcerptReport(cadastralNumber: "88005553535",
                                        createDate: creationDate,
                                        flatReport: flatExcerptReport)
        return report
    }

    private static let validArea: Double = 100
    private static let validFloorString = "1"

    // swiftlint:disable:next closure_body_length
    private static let offer: YREOffer = {
        let offer = YREOffer(
            identifier: "",
            type: .sell,
            category: .apartment,
            partnerId: nil,
            internal: .paramBoolUnknown,
            creationDate: nil,
            newFlatSale: .paramBoolFalse,
            primarySale: .paramBoolFalse,
            flatType: .secondary,
            urlString: nil,
            update: nil,
            roomsTotal: 3,
            roomsOffered: 3,
            floorsTotal: 30,
            floorsOffered: [1],
            author: nil,
            isFullTrustedOwner: .paramBoolTrue,
            trust: .yreConstantParamOfferTrustHigh,
            viewsCount: 0,
            floorCovering: .unknown,
            area: YREArea(unit: .m2, value: 100),
            livingSpace: nil,
            kitchenSpace: nil,
            roomSpace: nil,
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
            suspicious: .paramBoolUnknown,
            active: .paramBoolUnknown,
            hasAlarm: .paramBoolUnknown,
            housePart: .paramBoolUnknown,
            price: nil,
            offerPriceInfo: nil,
            location: nil,
            metro: nil,
            house: nil,
            building: nil,
            garage: nil,
            lotArea: nil,
            lotType: .unknown,
            offerDescription: nil,
            commissioningDate: nil,
            ceilingHeight: nil,
            haggle: .paramBoolUnknown,
            mortgage: .paramBoolUnknown,
            rentPledge: .paramBoolUnknown,
            electricityIncluded: .paramBoolUnknown,
            cleaningIncluded: .paramBoolUnknown,
            withKids: .paramBoolUnknown,
            withPets: .paramBoolUnknown,
            supportsOnlineView: .paramBoolUnknown,
            zhkDisplayName: nil,
            apartment: nil,
            dealStatus: .primarySale,
            agentFee: nil,
            commission: nil,
            prepayment: nil,
            securityPayment: nil,
            taxationForm: .unknown,
            isFreeReportAvailable: .paramBoolTrue,
            isPurchasingReportAvailable: .paramBoolTrue,
            paidExcerptsInfo: nil,
            generatedFromSnippet: false,
            heating: .paramBoolUnknown,
            water: .paramBoolUnknown,
            sewerage: .paramBoolUnknown,
            electricity: .paramBoolUnknown,
            gas: .paramBoolUnknown,
            utilitiesIncluded: .paramBoolUnknown,
            salesDepartments: nil,
            isOutdated: false,
            uid: nil,
            share: nil,
            enrichedFields: nil,
            history: nil,
            commercialDescription: nil,
            villageInfo: nil,
            siteInfo: nil,
            vas: nil,
            queryContext: nil,
            chargeForCallsType: .noCharge,
            yandexRent: .paramBoolUnknown,
            userNote: nil,
            virtualTours: nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )

        return offer
    }()
}
