//
//  OfferCardYandexRentPromoViewTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 20.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREModelObjc
import XCTest
import YRETestsUtils
@testable import YREOfferCardModule

final class OfferCardYandexRentPromoViewTests: XCTestCase {
    func testView() {
        let offer = Self.makeOffer()
        let viewModel = YREUnwrap(OfferCardYandexRentPromoNodeViewModelGenerator.make(offer: offer))

        let width = UIScreen.main.bounds.width
        let height = OfferCardYandexRentPromoView.height(width: width, viewModel: viewModel)

        let view = OfferCardYandexRentPromoView()
        view.configure(viewModel: viewModel)
        view.frame = CGRect(x: 0.0, y: 0.0, width: width, height: height)

        self.assertSnapshot(view)
    }

    // swiftlint:disable:next function_body_length
    private static func makeOffer() -> YREOffer {
        YREOffer(
            identifier: "",
            type: .unknown,
            category: .unknown,
            partnerId: nil,
            internal: .paramBoolFalse,
            creationDate: Date(),
            newFlatSale: .paramBoolUnknown,
            primarySale: .paramBoolUnknown,
            flatType: .unknown,
            urlString: nil,
            update: nil,
            roomsTotal: 2,
            roomsOffered: 1,
            floorsTotal: 12,
            floorsOffered: nil,
            author: nil,
            isFullTrustedOwner: .paramBoolUnknown,
            trust: .yreConstantParamOfferTrustUnknown,
            viewsCount: 0,
            floorCovering: .unknown,
            area: nil,
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
            isFreeReportAvailable: .paramBoolUnknown,
            isPurchasingReportAvailable: .paramBoolUnknown,
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
            chargeForCallsType: .unknown,
            yandexRent: .paramBoolTrue,
            userNote: nil,
            virtualTours: nil,
            offerChatType: .unknown,
            hasPaidCalls: .paramBoolUnknown
        )
    }
}
