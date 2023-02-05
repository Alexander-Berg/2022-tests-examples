//
//  SelectedFoldersTest.swift
//  SelectedFoldersTest
//
//  Created by Timur Turaev on 19.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal final class SelectedFoldersTest: XCTestCase, AvailableBackupFoldersDataSource {
    private var loader: SelectedBackupFoldersLoader!

    private func initEnvironment(withTabs: Bool) {
        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: withTabs)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        self.loader = SelectedBackupFoldersLoader(httpClient: httpClient,
                                                  availableBackupFoldersDataSource: self)
    }

    func testLoadingSelectedBackupFoldersWithTabs() throws {
        self.performLoadingSelectedBackupFolders(withTabs: true)
    }

    func testLoadingSelectedBackupFoldersNoTabs() throws {
        self.performLoadingSelectedBackupFolders(withTabs: false)
    }

    private func performLoadingSelectedBackupFolders(withTabs: Bool) {
        initEnvironment(withTabs: withTabs)

        let exp = self.expectation(description: #function)
        self.loader.loadSelectedBackupFolder { result in
            let selectedGroups = try! XCTUnwrap(result.toOptional())

            XCTAssertEqual(selectedGroups.count, 3)
            XCTAssertTrue(selectedGroups.allSatisfy { $0.folders.count == 1 })

            XCTAssertEqual(selectedGroups[0].folders.first!.isSelected, withTabs)
            XCTAssertTrue(selectedGroups[1].folders.first!.isSelected)
            XCTAssertFalse(selectedGroups[2].folders.first!.isSelected)

            exp.fulfill()
        }

        self.wait(for: [exp], timeout: 1)
    }

    // swiftlint:disable:next test_case_accessibility
    func fetchFoldersAvailableForBackup(completion: @escaping (Result<[BackupFoldersGroup]>) -> Void) {
        DispatchQueue.global().asyncAfter(deadline: .now() + .milliseconds(20)) {
            let groups = [
                BackupFoldersGroup(folders: [BackupFolder(id: -10, name: "TabInbox", messagesCount: 2, indent: 0)]),
                BackupFoldersGroup(folders: [BackupFolder(id: 12, name: "User", messagesCount: 3, indent: 0)]),
                BackupFoldersGroup(folders: [BackupFolder(id: 10, name: "User2", messagesCount: 3, indent: 0)])
            ]
            completion(.success(groups))
        }
    }
}
