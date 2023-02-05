//
//  Created by Timur Turaev on 28.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
import Utils
import TurboApp
@testable import Taverna

internal final class TavernaBundleStorageTests: XCTestCase {
    private var configuration: TestConfiguration!

    override func setUp() {
        self.configuration = TestConfiguration()

        FileManager.default.recreateDirectory(at: self.configuration.cacheFolderURL)
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    func testInitializingDefaultTaverna() throws {
        let storage = TavernaBundleStorage(configuration: self.configuration!)

        let expectation = XCTestExpectation(description: #function)
        storage.fetchResourceBundle(completionQueue: DispatchQueue.main) { bundleResult in
            guard let taverna = bundleResult.toOptional() else {
                XCTFail("Cannot fetch default taverna from bundle: \(bundleResult.toError()!)")
                fatalError("unreachable")
            }
            XCTAssertNotNil(taverna.manifest.remoteURL)
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestConfiguration: MayaBundleStorageConfiguration {
    var folderName = "Taverna"
    
    var cacheFolderURL: URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent("TavernaTests", isDirectory: true)
    }

    var manifestConfiguration: MayaManifestConfiguration {
        return TestManifestConfiguration()
    }

    var forceUseBundledManifest: Bool {
        return false
    }
}

private struct TestManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}
