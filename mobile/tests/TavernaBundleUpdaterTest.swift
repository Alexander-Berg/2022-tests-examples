//
//  Created by Timur Turaev on 29.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import Foundation

import XCTest
import Utils
import TurboApp
@testable import Taverna

internal final class TavernaBundleUpdaterTests: XCTestCase {
    
    private var configuration: TavernaBundleUpdaterConfiguration!
    private var lastTimeUpdater: TestLastTimeUpdater!
    private var updater: TavernaBundleUpdater!

    override func setUp() {
        super.setUp()

        self.lastTimeUpdater = TestLastTimeUpdater()
        self.configuration = TavernaBundleUpdaterConfiguration(lastTimeUpdater: lastTimeUpdater)
        
        let storageConfiguration = TestBundleStorageConfiguration()
        
        FileManager.default.recreateDirectory(at: storageConfiguration.cacheFolderURL)
        
        let bundleURL = Bundle.tavernaResources.url(forResource: "Taverna", withExtension: "")!
        
        self.updater = TavernaBundleUpdater(configuration: self.configuration,
                                            storage: TestBundleStorage(cacheURL: storageConfiguration.cacheFolderURL,
                                                                       bundleURL: bundleURL,
                                                                       bundleInitializer: TestTavernaBundleInitializer(),
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

        NotificationCenter.default.post(name: TavernaBundleUpdater.Constants.updateNotificationName, object: nil)
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

        NotificationCenter.default.post(name: TavernaBundleUpdater.Constants.updateNotificationName, object: nil)
        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestTavernaBundleInitializer: ResourceBundleInitializer {
    let newVersionManifestFileName = "manifest.json"
    func initializeManifest(from data: Data) throws -> TavernaManifest {
        return TavernaManifest(info: .init(cache: .init(files: []),
                                           version: "10.2.3",
                                           prefetch: [ .init(name: "manifest", url: "badschema://myremoteurl", period: 10)]),
                               startURL: "https://calendar.yandex.ru/manifest",
                               indexPageURL: URL(string: "https://calendar.yandex.ru/manifest")!
        )
    }
    
    func initializeResourceBundle(from url: URL) throws -> TavernaBundle {
        return TavernaBundle(manifest: try! self.initializeManifest(from: Data(capacity: 1)),
                             manifestConfiguration: TestTavernaManifestConfiguration(),
                             manifestLocalURL: nil,
                             startPageURL: URL(string: "https://calendar.yandex.ru/taverna/manifest")!,
                             staticFilesMapping: [:])
    }
}

private final class TestBundleStorage: MayaBundleStorage<TavernaBundle> {}

private final class TestBundleStorageConfiguration: MayaBundleStorageConfiguration {
    var folderName: String = "Taverna"
    
    var cacheFolderURL: URL {
        return FileManager.default.temporaryDirectory.appendingPathComponent("TavernaTests", isDirectory: true)
    }

    var manifestConfiguration: MayaManifestConfiguration {
        return TestTavernaManifestConfiguration()
    }

    var forceUseBundledManifest: Bool {
        return false
    }
}

private class TestLastTimeUpdater: MayaBundleLastTimeUpdater {
    var lastUpdateTime = Date()
}

private struct TestTavernaManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}

internal func YOXCTFail(_ message: String, file: StaticString = #file, line: UInt = #line) -> Never {
    XCTFail(message, file: file, line: line)
    fatalError("unreachable")
}
