//
//  Created by Timur Turaev on 29.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import Foundation

import XCTest
import Utils
import TurboApp
@testable import ContactCard

internal final class ContactCardBundleUpdaterTests: XCTestCase {
    
    private var configuration: ContactCardBundleUpdaterConfiguration!
    private var lastTimeUpdater: TestLastTimeUpdater!
    private var updater: ContactCardBundleUpdater!

    override func setUp() {
        super.setUp()

        self.lastTimeUpdater = TestLastTimeUpdater()
        self.configuration = ContactCardBundleUpdaterConfiguration(lastTimeUpdater: lastTimeUpdater)
        
        let storageConfiguration = TestBundleStorageConfiguration()
        
        FileManager.default.recreateDirectory(at: storageConfiguration.cacheFolderURL)
        
        let bundleURL = Bundle.contactCardResources.url(forResource: "ContactCard", withExtension: "")!
        
        self.updater = ContactCardBundleUpdater(configuration: self.configuration,
                                                storage: TestContactCardBundleStorage(cacheURL: storageConfiguration.cacheFolderURL,
                                                                                      bundleURL: bundleURL,
                                                                                      bundleInitializer: TestContactCardBundleInitializer(),
                                                                                      forceUseBundledManifest: false),
                                                customManifestRemoteURL: nil,
                                                authChallengeDisposition: .default)
    }

    override func tearDown() {
        self.updater = nil
        super.tearDown()
    }

    func testNormalUpdating() throws {
        self.lastTimeUpdater.lastUpdateTime.addTimeInterval(-11 * 60) // -11 minutes

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            // is't ok, trying to load resources, updating completed
            guard case .failure(.downloadingRemoteManifestFailed) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            expectation.fulfill()
        }

        NotificationCenter.default.post(name: ContactCardBundleUpdater.Constants.updateNotificationName, object: nil)
        self.wait(for: [expectation], timeout: 5)
    }

    func testUpdatingToSoon() throws {
        self.lastTimeUpdater.lastUpdateTime.addTimeInterval(-5 * 60) // -5 minutes

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.updatingTooFrequent) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            expectation.fulfill()
            expectation.fulfill()
        }

        NotificationCenter.default.post(name: ContactCardBundleUpdater.Constants.updateNotificationName, object: nil)
        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestContactCardBundleInitializer: ResourceBundleInitializer {
    let newVersionManifestFileName = "manifest.json"
    func initializeManifest(from data: Data) throws -> ContactCardManifest {
        return ContactCardManifest(info: .init(cache: .init(files: []),
                                               version: "10.2.3",
                                               prefetch: [ .init(name: "manifest", url: "badschema://myremoteurl", period: 10)]),
                                   startURL: "https://mail.yandex.ru",
                                   indexPageURL: URL(string: "https://calendar.yandex.ru/contact-card/manifest")!)
    }
    
    func initializeResourceBundle(from url: URL) throws -> ContactCardBundle {
        return ContactCardBundle(manifest: try! self.initializeManifest(from: Data(capacity: 1)),
                                 manifestConfiguration: TestContactCardManifestConfiguration(),
                                 manifestLocalURL: nil,
                                 startPageURL: URL(string: "https://calendar.yandex.ru/contact-card/manifest")!,
                                 staticFilesMapping: [:])
    }
}

private final class TestContactCardBundleStorage: MayaBundleStorage<ContactCardBundle> {}

private final class TestBundleStorageConfiguration: MayaBundleStorageConfiguration {
    var folderName: String = "ContactCard"
    
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

private class TestLastTimeUpdater: MayaBundleLastTimeUpdater {
    var lastUpdateTime = Date()
}

private struct TestContactCardManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}

internal func YOXCTFail(_ message: String, file: StaticString = #file, line: UInt = #line) -> Never {
    XCTFail(message, file: file, line: line)
    fatalError("unreachable")
}
