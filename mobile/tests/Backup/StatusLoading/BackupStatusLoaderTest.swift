//
//  BackupStatusLoaderTest.swift
//  BackupStatusLoaderTest
//
//  Created by Timur Turaev on 18.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal final class BackupStatusLoaderTest: XCTestCase {
    private var loadedStatus: BackupStatus!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
        //            .realAndCapture(withTabs: false)
        //            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let future = httpClient.runRequest(BackupStatusRequest(), responseType: BackupStatusResponse.self).receiveOnMainQueue()
        self.loadedStatus = try self.waitFor(future).get()
    }

    func testLoadingEmptyStatus() throws {
        XCTAssertEqual(self.loadedStatus, BackupStatus(currentBackup: .nothing,
                                                       hasBeingCreatedBackup: false,
                                                       restoringBackup: .nothing))
    }

    func testLoadingExistingBackupStatus() throws {
        XCTAssertEqual(self.loadedStatus, BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_629_281_235)),
                                                       hasBeingCreatedBackup: false,
                                                       restoringBackup: .nothing))
    }

    func testLoadingBeingCreatedBackupStatus() throws {
        XCTAssertEqual(self.loadedStatus, BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_613_313_649)),
                                                       hasBeingCreatedBackup: true,
                                                       restoringBackup: .nothing))
    }

    func testLoadingOnlyBeingCreatedBackupStatus() throws {
        XCTAssertEqual(self.loadedStatus, BackupStatus(currentBackup: .nothing,
                                                       hasBeingCreatedBackup: true,
                                                       restoringBackup: .nothing))
    }

    func testLoadingBeingCreatedWithErrorBackupStatus() throws {
        XCTAssertEqual(self.loadedStatus, BackupStatus(currentBackup: .exist(created: Date(timeIntervalSince1970: 1_613_313_649)),
                                                       hasBeingCreatedBackup: false,
                                                       restoringBackup: .exist(restoredMessages: 123,
                                                                               totalMessages: 666,
                                                                               inProgress: true,
                                                                               method: .restoredFolder)))
    }
}
