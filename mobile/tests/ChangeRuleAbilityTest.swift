//
//  ChangeRuleAbilityTest.swift
//  FiltersTests
//
//  Created by Timur Turaev on 11.11.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

internal final class ChangeRuleAbilityTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!
    private let rule = RuleModel(id: 613_616, name: "1", isEnabled: false, stop: false, logic: .and, conditions: .empty, actions: .empty, flags: .empty)

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testEnablingRule() throws {
        let future = self.httpClient.runRequest(ChangeRuleAbilityRequest(rule: self.rule, enable: true), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }

    func testDisablingRule() throws {
        let future = self.httpClient.runRequest(ChangeRuleAbilityRequest(rule: self.rule, enable: false), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }
}
