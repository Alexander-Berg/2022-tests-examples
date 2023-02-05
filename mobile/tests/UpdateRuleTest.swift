//
//  UpdateRuleTest.swift
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
internal final class UpdateRuleTest: XCTestCase {
    private var filtersList: [RuleModel]!
    private var httpClient: HTTPClientProtocol!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)

        let future = self.httpClient.runRequest(FiltersListRequest(force: false), responseType: FiltersListResponse.self).receiveOnMainQueue()
        let filtersDTOList = try self.waitFor(future).get()

        self.filtersList = filtersDTOList.rules.compactMap { $0.convertToModel(containersDataSource: TestContainersDataSource()) }
        XCTAssertEqual(self.originalRule.kind, .onlyFrom)
        XCTAssertEqual(self.originalRule, .init(id: 387_686, name: "69_name!", isEnabled: true, stop: false, logic: .or,
                                                conditions: [
                                                    .init(key: .from, operator: .contains, value: "from2"),
                                                    .init(key: .from, operator: .contains, value: "from1")
                                                ],
                                                actions: [
                                                    .applyLabel(label: RuleModel.Container(id: 7, name: "7")),
                                                    .markRead
                                                ],
                                                flags: [.spam(.no)]))
    }

    func testUpdatingRule() throws {
        let newRule = RuleModel(id: self.originalRule.id,
                                name: self.originalRule.name + "_",
                                isEnabled: true,
                                stop: true,
                                logic: .and,
                                conditions: [
                                    .init(key: .subject, operator: .contains, value: "SUBJ"),
                                    .init(key: .from, operator: .contains, value: "FROM")
                                ], actions: [
                                    .moveToFolder(folder: RuleModel.Container(id: 30, name: "30")),
                                    .applyLabel(label: RuleModel.Container(id: 41, name: "41"))
                                ], flags: [.spam(.no)])
        XCTAssertEqual(newRule.kind, .mixed)

        let future = self.httpClient.runRequest(CreateUpdateFilterRuleRequest(rule: newRule),
                                                responseType: CreateUpdateFilterRuleResponse.self).receiveOnMainQueue()
        let newID = try self.waitFor(future).get()

        // read new rule from server
        let readFiltersListFuture = self.httpClient.runRequest(FiltersListRequest(force: false), responseType: FiltersListResponse.self).receiveOnMainQueue()
        let ruleFromServer = try self.waitFor(readFiltersListFuture)
            .get()
            .rules.compactMap { $0.convertToModel(containersDataSource: TestContainersDataSource()) }
            .first!

        // orders in actions and conditions are not determined
        XCTAssertEqual(ruleFromServer, RuleModel(id: newID,
                                                 name: newRule.name,
                                                 isEnabled: newRule.isEnabled,
                                                 stop: newRule.stop,
                                                 logic: .and,
                                                 conditions: [
                                                    .init(key: .from, operator: .contains, value: "FROM"),
                                                    .init(key: .subject, operator: .contains, value: "SUBJ")
                                                 ],
                                                 actions: [
                                                    .applyLabel(label: RuleModel.Container(id: 41, name: "41")),
                                                    .moveToFolder(folder: RuleModel.Container(id: 30, name: "30"))
                                                 ], flags: newRule.flags))
    }

    private var originalRule: RuleModel {
        return self.filtersList.first!
    }
}
