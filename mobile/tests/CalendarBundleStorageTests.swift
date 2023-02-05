//
//  Created by Timur Turaev on 28.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
import Utils
import TurboApp
@testable import OfflineCalendar

internal final class CalendarBundleStorageTests: XCTestCase {
    private var configuration: TestConfiguration!

    override func setUp() {
        self.configuration = TestConfiguration()

        FileManager.default.recreateDirectory(at: self.configuration.cacheFolderURL)
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    func testInitializingDefaultCalendar() throws {
        let storage = CalendarBundleStorage(configuration: self.configuration!)

        let expectation = XCTestExpectation(description: #function)
        storage.fetchResourceBundle(completionQueue: DispatchQueue.main) { calendarBundle in
            guard let calendar = calendarBundle.toOptional() else {
                XCTFail("Cannot fetch default calendar from bundle: \(calendarBundle.toError()!)")
                fatalError("unreachable")
            }
            XCTAssertNotNil(calendar.manifest.remoteURL)
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestConfiguration: MayaBundleStorageConfiguration {
    var folderName = "OfflineCalendar"
    
    var cacheFolderURL: URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent("OfflineCalendarTests", isDirectory: true)
    }

    var manifestConfiguration: MayaManifestConfiguration {
        return TestCalendarManifestConfiguration()
    }

    var forceUseBundledManifest: Bool {
        return false
    }
}

private struct TestCalendarManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}
