//
//  DeleteFilterRuleTest.swift
//  FiltersTests
//
//  Created by Timur Turaev on 11.11.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

internal final class DeleteFilterRuleTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!
    private let rule = RuleModel(id: 1, name: "1", isEnabled: false, stop: false, logic: .and, conditions: .empty, actions: .empty, flags: .empty)

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
        let future = self.httpClient.runRequest(DeleteFilterRuleRequest(rule: self.rule), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }
}
