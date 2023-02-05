//
//  EnotLoaderTest.swift
//  EnotLoaderTest
//
//  Created by Timur Turaev on 11.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
@testable import Backup

internal final class EnotLoaderTest: XCTestCase {
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

    func testCreatingHiddenTrash() throws {
        let future = self.httpClient.runRequest(CreateHiddenTrashRequest(), responseType: CreateHiddenTrashResponse.self)
            .receiveOnMainQueue()
        let hiddenTrashFolderID = try self.waitFor(future).get()
        XCTAssertEqual(hiddenTrashFolderID, 9)
    }

    func testDeletingHiddenTrash() throws {
        let future = self.httpClient.runRequest(DeleteHiddenTrashRequest(), responseType: EmptyResponse.self)
            .receiveOnMainQueue()
        try self.waitFor(future)
    }
}
