//
//  AnyOfferImagesProviderTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 05.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import YREModelObjc
import YREModel
@testable import YREComponents

final class AnyOfferImagesProviderTests: XCTestCase {
    func testSiteSnippetEmptyImagesLarge1242Size() {
        let size = ImagePipeline.Alias.large1242.sizeInPixels
        let snippet = Self.makeSiteSnippet(
            large1242ImageURLs: [],
            largeImageURLs: [],
            middleImageURLs: [],
            fullImageURLs: []
        )

        let images = AnyOfferImagesProvider.getImageURLsFittingSize(size, for: snippet)

        XCTAssertTrue(images.isEmpty)
    }

    func testSiteSnippetOnlyLarge1242ImagesLarge1242Size() {
        let size = ImagePipeline.Alias.large1242.sizeInPixels
        let snippet = Self.makeSiteSnippet(
            large1242ImageURLs: [YREUnwrap(URL(string: "https://large1242.image"))],
            largeImageURLs: [],
            middleImageURLs: [],
            fullImageURLs: []
        )

        let images = AnyOfferImagesProvider.getImageURLsFittingSize(size, for: snippet)

        XCTAssertEqual(images, [YREUnwrap(URL(string: "https://large1242.image"))])
    }

    func testSiteSnippetOnlyLargeImagesLarge1242Size() {
        let size = ImagePipeline.Alias.large1242.sizeInPixels
        let snippet = Self.makeSiteSnippet(
            large1242ImageURLs: [],
            largeImageURLs: [YREUnwrap(URL(string: "https://large.image"))],
            middleImageURLs: [],
            fullImageURLs: []
        )

        let images = AnyOfferImagesProvider.getImageURLsFittingSize(size, for: snippet)

        XCTAssertEqual(images, [YREUnwrap(URL(string: "https://large.image"))])
    }

    func testSiteSnippetOnlyMiddleImagesLarge1242Size() {
        let size = ImagePipeline.Alias.large1242.sizeInPixels
        let snippet = Self.makeSiteSnippet(
            large1242ImageURLs: [],
            largeImageURLs: [],
            middleImageURLs: [YREUnwrap(URL(string: "https://middle.image"))],
            fullImageURLs: []
        )

        let images = AnyOfferImagesProvider.getImageURLsFittingSize(size, for: snippet)

        XCTAssertEqual(images, [YREUnwrap(URL(string: "https://middle.image"))])
    }

    func testSiteSnippetOnlyFullImagesLarge1242Size() {
        let size = ImagePipeline.Alias.large1242.sizeInPixels
        let snippet = Self.makeSiteSnippet(
            large1242ImageURLs: [],
            largeImageURLs: [],
            middleImageURLs: [],
            fullImageURLs: [YREUnwrap(URL(string: "https://full.image"))]
        )

        let images = AnyOfferImagesProvider.getImageURLsFittingSize(size, for: snippet)

        XCTAssertEqual(images, [YREUnwrap(URL(string: "https://full.image"))])
    }
}

extension AnyOfferImagesProviderTests {
    private static func makeSiteSnippet(
        large1242ImageURLs: [URL]?,
        largeImageURLs: [URL]?,
        middleImageURLs: [URL]?,
        fullImageURLs: [URL]?
    ) -> YRESiteSnippet {
        YRESiteSnippet(
            identifier: "",
            name: nil,
            shortName: nil,
            large1242ImageURLs: large1242ImageURLs,
            largeImageURLs: largeImageURLs,
            middleImageURLs: middleImageURLs,
            fullImageURLs: fullImageURLs,
            location: nil,
            metro: nil,
            filterStatistics: nil,
            resaleFilterStatistics: nil,
            priceInfo: nil,
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
