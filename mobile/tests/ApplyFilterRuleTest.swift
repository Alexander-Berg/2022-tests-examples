//
//  ApplyFilterRuleTest.swift
//  FiltersTests
//
//  Created by Timur Turaev on 11.11.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

internal final class ApplyFilterRuleTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testDeletingRule() throws {
        let future = self.httpClient.runRequest(ApplyFilterRuleRequest(ruleID: 1), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }
}
