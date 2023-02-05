//
//  CreateRuleTest.swift
//  FiltersTests
//
//  Created by Timur Turaev on 12.11.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

// swiftlint:disable multiline_arguments
internal final class CreateRuleTest: XCTestCase {
    private var id: YOIDType!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let rule = RuleModel(id: nil, name: "69_name!", isEnabled: true, stop: false, logic: .or,
                             conditions: [
                                .init(key: .from, operator: .contains, value: "from2"),
                                .init(key: .from, operator: .contains, value: "from1")
                             ],
                             actions: [
                                .applyLabel(label: RuleModel.Container(id: 7, name: "7")),
                                .markRead
                             ],
                             flags: [.spam(.no)])

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let future = httpClient.runRequest(CreateUpdateFilterRuleRequest(rule: rule), responseType: CreateUpdateFilterRuleResponse.self).receiveOnMainQueue()
        self.id = try self.waitFor(future).get()
    }

    func testCreatingRule() throws {
        XCTAssertEqual(self.id, 387_698)
    }
}
