//
//  OfferSnippetSearchResultsGenerators.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Pavel Zhuravlev on 05.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREModel
import YREModelObjc

final class OfferSnippetSearchResultsGenerator: IteratorProtocol {
    typealias Element = YREOfferSnippet

    let itemsCount: Int

    init(itemsCount: Int) {
        self.itemsCount = itemsCount
    }

    func next() -> YREOfferSnippet? {
        guard self.createdItemsCount < self.itemsCount else { return nil }

        self.createdItemsCount += 1
        return Self.makeOffer(id: "\(self.createdItemsCount)")
    }

    // MARK: Private

    private var createdItemsCount: Int = 0

    private static func makeOffer(id: String) -> YREOfferSnippet {
        let offer = YREOfferSnippet(
            identifier: id,
            type: .unknown,
            category: .unknown,
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
            price: nil,
            offerPriceInfo: nil,
            vas: nil,
            roomsOffered: 0,
            roomsTotal: 0,
            area: nil,
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
