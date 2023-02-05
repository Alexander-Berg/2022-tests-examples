//
//  Created by Timur Turaev on 28.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
import Utils
import TurboApp
@testable import ContactCard

internal final class ContactCardBundleStorageTests: XCTestCase {
    private var configuration: TestConfiguration!

    override func setUp() {
        self.configuration = TestConfiguration()

        FileManager.default.recreateDirectory(at: self.configuration.cacheFolderURL)
        super.setUp()
    }

    override func tearDown() {
        super.tearDown()
    }

    func testInitializingDefaultContactCard() throws {
        let storage = ContactCardBundleStorage(configuration: self.configuration!)

        let expectation = XCTestExpectation(description: #function)
        storage.fetchResourceBundle(completionQueue: DispatchQueue.main) { contactCardBundle in
            guard let contactCard = contactCardBundle.toOptional() else {
                XCTFail("Cannot fetch default contact card from bundle: \(contactCardBundle.toError()!)")
                fatalError("unreachable")
            }
            XCTAssertNotNil(contactCard.manifest.remoteURL)
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestConfiguration: MayaBundleStorageConfiguration {
    var folderName = "ContactCard"
    
    var cacheFolderURL: URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent("ContactCardTests", isDirectory: true)
    }

    var manifestConfiguration: MayaManifestConfiguration {
        return TestContactCardManifestConfiguration()
    }

    var forceUseBundledManifest: Bool {
        return false
    }
}

private struct TestContactCardManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}
