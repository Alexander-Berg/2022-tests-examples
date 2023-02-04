//
//  DeepLinkServiceTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 09.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREWeb
@testable import YRELegacyFiltersCore
@testable import YREAppState
@testable import YREServiceLayer

final class DeepLinkServiceTests: XCTestCase {
    override func setUp() {
        super.setUp()

        let paidLoopStateWriter = PaidLoopStateWriterMock()
        let factory = WebServicesFactoryMock()
        let service = DeepLinkService(
            webServices: factory,
            filterRootFactory: FilterRootFactory(),
            paidLoopStateWriter: paidLoopStateWriter,
            appSessionState: AppSessionStateWriterMock(),
            frontendURLProvider: FrontendURLProvider(connectionSettings: ConnectionSettingsMock())
        )

        self.paidLoopStateWriter = paidLoopStateWriter
        self.factory = factory
        self.service = service
    }

    override func tearDown() {
        super.tearDown()

        self.paidLoopStateWriter = nil
        self.factory = nil
        self.service = nil
    }

    func testPaidLoopStateWriter() {
        let expectation = XCTestExpectation(description: "Deeplink parsing")
        let expectedAdSource = "yandex_direct"
        let expectedUTMCampaign = "460_56887073_poisk_tgo_msk_sell_obshie"

        self.factory?.deepLink.result = [
            "action": "OFFER_LIST",
            "filter": [:],
            "params": [
                ["name": "ad_source", "values": [expectedAdSource]],
                ["name": "utm_content", "values": ["idgr-4367574770_cat-kupit_kvartiru_newbuilding_obshie_msk"]],
                ["name": "utm_campaign", "values": [expectedUTMCampaign]],
                ["name": "free_contour", "values": ["1"]],
            ],
        ]

        self.service?.parse(
            URL(string: "https://")!, // swiftlint:disable:this force_unwrapping
            completionBlock: { _, _ in expectation.fulfill() }
        )

        self.wait(for: [expectation], timeout: Const.expectationTimeout)
        XCTAssertEqual(self.paidLoopStateWriter?.events, [.updatePaidLoopAdSourceCall])
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.adSource, expectedAdSource)
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.utmCampaign, expectedUTMCampaign)
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.isFreeContour, true)
    }

    func testPaidLoopStateWriterUpdateAdSource() {
        let expectation = XCTestExpectation(description: "Deeplink parsing")
        let expectedAdSource = "yandex_direct"

        self.factory?.deepLink.result = [
            "action": "OFFER_LIST",
            "filter": [:],
            "params": [
                ["name": "ad_source", "values": [expectedAdSource]],
            ],
        ]

        self.service?.parse(
            URL(string: "https://")!, // swiftlint:disable:this force_unwrapping
            completionBlock: { _, _ in expectation.fulfill() }
        )

        self.wait(for: [expectation], timeout: Const.expectationTimeout)
        XCTAssertEqual(self.paidLoopStateWriter?.events, [.updatePaidLoopAdSourceCall])
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.adSource, expectedAdSource)
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.utmCampaign, nil)
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo?.isFreeContour, false)
    }

    func testPaidLoopStateWriterWithoutAdSource() {
        let expectation = XCTestExpectation(description: "Deeplink parsing")

        self.factory?.deepLink.result = [
            "action": "OFFER_LIST",
            "filter": [:],
            "params": [
                // ["name": "ad_source", "values": [expectedAdSource]], // No ad_source
                ["name": "utm_campaign", "values": ["some_utm_campaign"]],
                ["name": "free_contour", "values": ["1"]],
            ],
        ]

        self.service?.parse(
            URL(string: "https://")!, // swiftlint:disable:this force_unwrapping
            completionBlock: { _, _ in expectation.fulfill() }
        )

        self.wait(for: [expectation], timeout: Const.expectationTimeout)
        XCTAssertEqual(self.paidLoopStateWriter?.paidLoopInfo, nil)
    }

    // MARK: - Private

    private enum Const {
        static let expectationTimeout: TimeInterval = 5.0
    }

    private var paidLoopStateWriter: PaidLoopStateWriterMock?
    private var factory: WebServicesFactoryMock?
    private var service: DeepLinkService?
}
