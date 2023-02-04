//
//  SiteCardAnchorsViewModelGeneratorTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 16.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModelObjc
import YRETestsUtils
@testable import YRESiteCardModule

final class SiteCardAnchorsViewModelGeneratorTests: XCTestCase {
    func testAllAnchorsOrder() {
        let site = Self.site(
            permalink: "что-то",
            tours: [
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .overview
                ),
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .tour
                ),
            ]
        )

        let anchors = SiteCardAnchorsViewModelGenerator.makeSectionAnchors(
            for: site,
            hasLocationSection: true,
            hasConstructionStateSection: true,
            hasSpecialProposalsSection: true,
            hasMortgageSection: true,
            hasDocumentsSection: true
        )

        let expectedAnchors: [SiteCardSectionAnchor] = [
            .tours,
            .overviews,
            .reviews,
            .location,
            .constructionStates,
            .specialProposals,
            .documentation,
        ]

        XCTAssertEqual(anchors, expectedAnchors)
    }

    func testAnchorsOrderWithoutMortgage() {
        let site = Self.site(
            permalink: "что-то",
            tours: [
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .overview
                ),
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .tour
                ),
            ]
        )

        let anchors = SiteCardAnchorsViewModelGenerator.makeSectionAnchors(
            for: site,
            hasLocationSection: true,
            hasConstructionStateSection: true,
            hasSpecialProposalsSection: true,
            hasMortgageSection: false,
            hasDocumentsSection: true
        )

        let expectedAnchors: [SiteCardSectionAnchor] = [
            .tours,
            .overviews,
            .reviews,
            .location,
            .constructionStates,
            .specialProposals,
            .documentation,
        ]

        XCTAssertEqual(anchors, expectedAnchors)
    }

    func testAnchorsOrderWithoutSpecialProposals() {
        let site = Self.site(
            permalink: "что-то",
            tours: [
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .overview
                ),
                SiteTour3D(
                    description: "",
                    link: YREUnwrap(URL(string: "https://")),
                    preview: YREUnwrap(URL(string: "https://")),
                    type: .tour
                ),
            ]
        )

        let anchors = SiteCardAnchorsViewModelGenerator.makeSectionAnchors(
            for: site,
            hasLocationSection: true,
            hasConstructionStateSection: true,
            hasSpecialProposalsSection: false,
            hasMortgageSection: true,
            hasDocumentsSection: true
        )

        let expectedAnchors: [SiteCardSectionAnchor] = [
            .tours,
            .overviews,
            .reviews,
            .location,
            .constructionStates,
            .specialProposals,
            .documentation,
        ]

        XCTAssertEqual(anchors, expectedAnchors)
    }

    // MARK: - Private. Factory

    private static func site(permalink: String, tours: [SiteTour3D]) -> YRESite {
        YRESite(
            identifier: "",
            name: "",
            shortName: "",
            locativeFullName: nil,
            update: nil,
            location: nil,
            metro: nil,
            url: nil,
            permalink: permalink,
            minicardImageURLs: nil,
            middleImageURLs: nil,
            largeImageURLs: nil,
            large1242ImageURLs: nil,
            fullImageURLs: nil,
            sourceNameURLs: nil,
            totalOffers: 0,
            avgPriceSqM: nil,
            priceRatioToMarket: nil,
            developers: nil,
            salesDepartments: nil,
            siteDescription: nil,
            deliveryDatesSummary: nil,
            filters: nil,
            resaleFilters: nil,
            roomStatistics: nil,
            resaleRoomStatistics: nil,
            filterStatistics: nil,
            resaleFilterStatistics: nil,
            priceInfo: nil,
            salesClosed: .paramBoolUnknown,
            flatStatus: .unknown,
            deliveryDates: nil,
            share: nil,
            specialProposals: nil,
            construction: nil,
            documents: nil,
            isOutdated: false,
            queryContext: nil,
            hasPaidCalls: .paramBoolUnknown,
            isLimitedCard: .paramBoolUnknown,
            video: nil,
            tours: tours,
            sitePlan: nil
        ) 
    }
}
