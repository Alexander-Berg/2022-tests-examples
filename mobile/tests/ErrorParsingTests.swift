//
//  ErrorParsingTests.swift
//  NetworkLayerTests
//
//  Created by Timur Turaev on 27.09.2021.
//

import Foundation

import XCTest
import TestUtils
@testable import NetworkLayer

internal final class ErrorParsingTests: XCTestCase {
    func testSendingAuthErrorNotification() throws {
        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "NO_TOKEN")
        let httpClient = builder.buildTestClient(kind: kind)

        let future = httpClient.runRequest(DevTestRequest(path: "register_domain"), responseType: DevTestResponse.self)
            .receiveOnMainQueue()

        let notificationExpectation = XCTNSNotificationExpectation(name: .authenticationError)
        let result = try self.waitFor(future)
        self.wait(for: [notificationExpectation], timeout: 1)

        result.onValue { _ in
            XCTFail("Should be error")
        }.onError { error in
            XCTAssertTrue(error.yo_isAuthError)
        }
    }
}
