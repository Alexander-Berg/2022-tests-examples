//
//  CreateBackupTest.swift
//  CreateBackupTest
//
//  Created by Timur Turaev on 18.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal class CreateBackupTest: XCTestCase {
    private var httpClient: HTTPClientProtocol!

    private func initEnvironment(withTabs: Bool) {
        let kind: TestHTTPClientKind =
//                    .realAndCapture(withTabs: withTabs)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        self.httpClient = builder.buildTestClient(kind: kind)
    }

    func testCreatingBackupAndLoadItsSettingsAndStatusNoTabs() throws {
        try self.runCreatingBackupAndLoadItsSettingsAndStatus(withTabs: false)
    }

    func testCreatingBackupAndLoadItsSettingsAndStatusWithTabs() throws {
        try self.runCreatingBackupAndLoadItsSettingsAndStatus(withTabs: true)
    }

    private func runCreatingBackupAndLoadItsSettingsAndStatus(withTabs: Bool) throws {
        initEnvironment(withTabs: withTabs)

        let timeout: TimeInterval = 1
        let fidsToCreateBackup: [YOIDType] = withTabs ? [12, -10] : [12]

        let createFuture = self.httpClient.runRequest(CreateBackupRequest(fids: fidsToCreateBackup), responseType: EmptyResponse.self)
            .receiveOnMainQueue()
        try self.waitFor(createFuture, timeout: timeout)

        let getStatusCreating = self.httpClient.runRequest(BackupStatusRequest(), responseType: BackupStatusResponse.self)
            .receiveOnMainQueue()
        let statusCreating = try self.waitFor(getStatusCreating, timeout: timeout).get()
        XCTAssertEqual(statusCreating, BackupStatus(currentBackup: .nothing,
                                                    hasBeingCreatedBackup: true,
                                                    restoringBackup: .nothing))

        let getStatusCompleted = self.httpClient.runRequest(BackupStatusRequest(), responseType: BackupStatusResponse.self)
            .receiveOnMainQueue()
        let statusCompleted = try self.waitFor(getStatusCompleted, timeout: timeout).get()
        XCTAssertEqual(statusCompleted, BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_629_310_115)),
                                                     hasBeingCreatedBackup: false,
                                                     restoringBackup: .nothing))

        let getSetings = self.httpClient.runRequest(BackupSettingsRequest(), responseType: BackupSettingsResponse.self)
            .receiveOnMainQueue()
        let actualFids: [YOIDType] = try self.waitFor(getSetings, timeout: timeout).get()
        let expectedFids = withTabs ? (fidsToCreateBackup + [1]).sorted() : fidsToCreateBackup
        XCTAssertEqual(actualFids, expectedFids)
    }
}
