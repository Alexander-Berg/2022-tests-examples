//
//  FiltersListTest.swift
//  FiltersTests
//
//  Created by Timur Turaev on 09.11.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Filters

// swiftlint:disable multiline_arguments
// swiftlint:disable function_body_length
internal final class FiltersListTest: XCTestCase {
    private var filtersDTOList: FiltersListResponseDTO!
    private var filtersList: [RuleModel]!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let future = httpClient.runRequest(FiltersListRequest(force: false), responseType: FiltersListResponse.self).receiveOnMainQueue()
        self.filtersDTOList = try self.waitFor(future).get()

        self.filtersList = self.filtersDTOList.rules.compactMap { $0.convertToModel(containersDataSource: TestContainersDataSource()) }
    }

    func testLoadingAndParsingFilters() throws {
        XCTAssertEqual(self.filtersDTOList.rules.count, 6)
        let qname3003 = "qname3003@yandex.ru"

        let r0 = RuleDTO(id: "613647", name: "Empty11", priority: 1, stop: true, enabled: true,
                         actions: [
                            .init(type: .notify, parameter: qname3003),
                            .init(type: .forwardWithStore, parameter: qname3003),
                            .init(type: .applyLabel, parameter: "7"),
                            .init(type: .status, parameter: "RO")
                         ],
                         conditions: [
                            .init(fieldType: .flag, div: .noSpam, operator: .equals, logic: .and, value: nil)
                         ])
        let r1 = RuleDTO(id: "613622", name: "Empty22", priority: 5, stop: false, enabled: false,
                         actions: [
                            .init(type: .applyLabel, parameter: "50"),
                            .init(type: .moveToFolder, parameter: "1")
                         ],
                         conditions: [
                            .init(fieldType: .flag, div: .hasAttachment, operator: .equals, logic: .and, value: nil),
                            .init(fieldType: .flag, div: .all, operator: .equals, logic: .and, value: nil)
                         ])
        let r2 = RuleDTO(id: "613616", name: "Empty33", priority: 9, stop: false, enabled: false,
                         actions: [
                            .init(type: .status, parameter: "RO")
                         ],
                         conditions: [
                            .init(fieldType: .flag, div: .hasAttachment, operator: .notEqual, logic: .and, value: nil),
                            .init(fieldType: .flag, div: .onylSpam, operator: .equals, logic: .and, value: nil)
                         ])
        let r3 = RuleDTO(id: "613629", name: "Delete", priority: 10, stop: false, enabled: true,
                         actions: [
                            .init(type: .status, parameter: "RO"),
                            .init(type: .delete, parameter: nil)
                         ],
                         conditions: [
                            .init(fieldType: .header, div: .from, operator: .contains, logic: .or, value: "from"),
                            .init(fieldType: .flag, div: .noSpam, operator: .equals, logic: .and, value: nil)
                         ])
        let r4 = RuleDTO(id: "613650", name: "All", priority: 11, stop: false, enabled: true,
                         actions: [
                            .init(type: .autoReply, parameter: "auto_reply_all"),
                            .init(type: .notify, parameter: qname3003),
                            .init(type: .forward, parameter: qname3003),
                            .init(type: .moveToFolder, parameter: "7"),
                            .init(type: .applyLabel, parameter: "7"),
                            .init(type: .status, parameter: "RO")
                         ],
                         conditions: [
                            .init(fieldType: .header, div: .cc, operator: .equals, logic: .or, value: "cc_e"),
                            .init(fieldType: .header, div: .to, operator: .equals, logic: .or, value: "to_e"),
                            .init(fieldType: .header, div: .toOrCC, operator: .equals, logic: .or, value: "tcc_e"),
                            .init(fieldType: .header, div: .from, operator: .equals, logic: .or, value: "from_e"),
                            .init(fieldType: .header, div: .subject, operator: .equals, logic: .or, value: "subj_e"),
                            .init(fieldType: .body, div: .body, operator: .notEqual, logic: .or, value: "body_ne"),
                            .init(fieldType: .attachment, div: .attachmentName, operator: .contains, logic: .or, value: "att_c"),
                            .init(fieldType: .header, div: .custom(value: "sender_name"), operator: .notContain, logic: .or, value: "header_nc"),
                            .init(fieldType: .flag, div: .noSpam, operator: .equals, logic: .and, value: nil)
                         ])
        let r5 = RuleDTO(id: "613642", name: "All ANDs", priority: 12, stop: true, enabled: true,
                         actions: [
                            .init(type: .status, parameter: "RO")
                         ],
                         conditions: [
                            .init(fieldType: .header, div: .from, operator: .contains, logic: .and, value: "1"),
                            .init(fieldType: .header, div: .from, operator: .contains, logic: .and, value: "2"),
                            .init(fieldType: .header, div: .from, operator: .contains, logic: .and, value: "3"),
                            .init(fieldType: .header, div: .from, operator: .contains, logic: .and, value: "4"),
                            .init(fieldType: .flag, div: .noSpam, operator: .equals, logic: .and, value: nil)
                         ])

        XCTAssertEqual(self.filtersDTOList.rules[0], r0)
        XCTAssertEqual(self.filtersDTOList.rules[1], r1)
        XCTAssertEqual(self.filtersDTOList.rules[2], r2)
        XCTAssertEqual(self.filtersDTOList.rules[3], r3)
        XCTAssertEqual(self.filtersDTOList.rules[4], r4)
        XCTAssertEqual(self.filtersDTOList.rules[5], r5)

        XCTAssertEqual(self.filtersList.count, 6)
        XCTAssertEqual(self.filtersList[0], .init(id: 613_647, name: "Empty11", isEnabled: true, stop: true, logic: .and,
                                                  conditions: [], actions: [
                                                    .notify(email: qname3003),
                                                    .forward(store: true, email: qname3003),
                                                    .applyLabel(label: RuleModel.Container(id: 7, name: "7")),
                                                    .markRead
                                                  ],
                                                  flags: [.spam(.no)]))
        XCTAssertEqual(self.filtersList[0].kind, .editOnDesktopOnly)
        XCTAssertEqual(self.filtersList[1], .init(id: 613_622, name: "Empty22", isEnabled: false, stop: false, logic: .and,
                                                  conditions: [], actions: [
                                                    .applyLabel(label: RuleModel.Container(id: 50, name: "50")),
                                                    .moveToFolder(folder: RuleModel.Container(id: 1, name: "1"))
                                                  ],
                                                  flags: [.attachment(has: true), .spam(.all)]))
        XCTAssertEqual(self.filtersList[1].kind, .editOnDesktopOnly)
        XCTAssertEqual(self.filtersList[2], .init(id: 613_616, name: "Empty33", isEnabled: false, stop: false, logic: .and,
                                                  conditions: [], actions: [
                                                    .markRead
                                                  ],
                                                  flags: [.attachment(has: false), .spam(.only)]))
        XCTAssertEqual(self.filtersList[2].kind, .editOnDesktopOnly)
        XCTAssertEqual(self.filtersList[3], .init(id: 613_629, name: "Delete", isEnabled: true, stop: false, logic: .or,
                                                  conditions: [
                                                    .init(key: .from, operator: .contains, value: "from")
                                                  ], actions: [
                                                    .markRead,
                                                    .delete
                                                  ],
                                                  flags: [.spam(.no)]))
        XCTAssertEqual(self.filtersList[3].kind, .onlyFrom)
        XCTAssertEqual(self.filtersList[4], .init(id: 613_650, name: "All", isEnabled: true, stop: false, logic: .or,
                                                  conditions: [
                                                    .init(key: .cc, operator: .equals, value: "cc_e"),
                                                    .init(key: .to, operator: .equals, value: "to_e"),
                                                    .init(key: .toOrCC, operator: .equals, value: "tcc_e"),
                                                    .init(key: .from, operator: .equals, value: "from_e"),
                                                    .init(key: .subject, operator: .equals, value: "subj_e"),
                                                    .init(key: .body, operator: .notEqual, value: "body_ne"),
                                                    .init(key: .attachmentName, operator: .contains, value: "att_c"),
                                                    .init(key: .header(name: "sender_name"), operator: .notContain, value: "header_nc")
                                                  ], actions: [
                                                    .autoReply(text: "auto_reply_all"),
                                                    .notify(email: qname3003),
                                                    .forward(store: false, email: qname3003),
                                                    .moveToFolder(folder: RuleModel.Container(id: 7, name: "7")),
                                                    .applyLabel(label: RuleModel.Container(id: 7, name: "7")),
                                                    .markRead
                                                  ],
                                                  flags: [.spam(.no)]))
        XCTAssertEqual(self.filtersList[4].kind, .editOnDesktopOnly)
        XCTAssertEqual(self.filtersList[5], .init(id: 613_642, name: "All ANDs", isEnabled: true, stop: true, logic: .and,
                                                  conditions: [
                                                    .init(key: .from, operator: .contains, value: "1"),
                                                    .init(key: .from, operator: .contains, value: "2"),
                                                    .init(key: .from, operator: .contains, value: "3"),
                                                    .init(key: .from, operator: .contains, value: "4")
                                                  ], actions: [
                                                    .markRead
                                                  ],
                                                  flags: [.spam(.no)]))
        XCTAssertEqual(self.filtersList[5].kind, .onlyFrom)
    }

    func testLoadingParsingUnknownCases() throws {
        XCTAssertEqual(self.filtersDTOList.rules, [
            .init(id: "1", name: "Empty33", priority: 9, stop: false, enabled: false,
                  actions: [
                    .init(type: .unknown(value: "AAA"), parameter: nil)
                  ],
                  conditions: [
                    .init(fieldType: .unknown(value: "FFF"),
                          div: .custom(value: "WAT"),
                          operator: .unknown(value: 222),
                          logic: .unknown(value: 111),
                          value: nil)
                  ])
        ])

        XCTAssertEqual(self.filtersList, [
            .init(id: 1, name: "Empty33", isEnabled: false, stop: false, logic: .and, conditions: .empty, actions: .empty, flags: .empty)
        ])
        XCTAssertEqual(self.filtersList.first!.kind, .editOnDesktopOnly)
    }
}
