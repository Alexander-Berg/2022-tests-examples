//
//  Created by Timur Turaev on 18.08.2021.
//

import Foundation
import XCTest
import TestUtils
import NetworkLayer
import Combine
@testable import Backup

internal final class LoadBackupSettingsTest: XCTestCase {
    private var selectedFids: [YOIDType]!

    override func setUpWithError() throws {
        try super.setUpWithError()

        let kind: TestHTTPClientKind =
//            .realAndCapture(withTabs: false)
//            .playFrom(nil)
            .playFrom(Bundle(for: Self.self))
        let builder = TestHTTPClientBuilder(fixtureName: self.defaultFixtureName, token: "~")
        let httpClient = builder.buildTestClient(kind: kind)

        let future = httpClient.runRequest(BackupSettingsRequest(), responseType: BackupSettingsResponse.self).receiveOnMainQueue()
        self.selectedFids = try self.waitFor(future).get()
    }

    func testLoadingSettingsWithoutTabs() throws {
        XCTAssertEqual(self.selectedFids, [1])
    }

    func testLoadingEmptySettings() throws {
        XCTAssertEqual(self.selectedFids, .empty)
    }

    func testLoadingSettingsWithTabs() throws {
        XCTAssertEqual(self.selectedFids, [-10, 1])
    }
}
