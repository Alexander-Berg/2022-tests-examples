//
//  DeleteBackupTest.swift
//  DeleteBackupTest
//
//  Created by Timur Turaev on 18.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal final class DeleteBackupTest: XCTestCase {
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

    func testDeletingBackup() throws {
        let deleteFuture = self.httpClient.runRequest(DeleteBackupRequest(), responseType: EmptyResponse.self)
            .receiveOnMainQueue()
        try self.waitFor(deleteFuture)
    }
}
