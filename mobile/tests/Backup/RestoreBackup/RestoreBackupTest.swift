//
//  RestoreBackupTest.swift
//  RestoreBackupTest
//
//  Created by Timur Turaev on 20.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal final class RestoreBackupTest: XCTestCase {
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

    func testRestoringFullHierarchy() throws {
        let future = self.httpClient.runRequest(RestoreBackupRequest(method: .fullHierarchy), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }

    func testRestoringToFolder() throws {
        let future = self.httpClient.runRequest(RestoreBackupRequest(method: .restoredFolder), responseType: EmptyResponse.self)
        try self.waitFor(future)
    }
}
