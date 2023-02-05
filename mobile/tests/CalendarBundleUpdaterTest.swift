//
//  Created by Timur Turaev on 29.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import Foundation

import XCTest
import Utils
import TurboApp
@testable import OfflineCalendar

internal final class CalendarBundleUpdaterTests: XCTestCase {
    
    private var configuration: CalendarBundleUpdaterConfiguration!
    private var lastTimeUpdater: TestLastTimeUpdater!
    private var updater: CalendarBundleUpdater!

    override func setUp() {
        super.setUp()

        self.lastTimeUpdater = TestLastTimeUpdater()
        self.configuration = CalendarBundleUpdaterConfiguration(lastTimeUpdater: lastTimeUpdater)
        
        let storageConfiguration = TestBundleStorageConfiguration()
        
        FileManager.default.recreateDirectory(at: storageConfiguration.cacheFolderURL)
        
        let bundleURL = Bundle.offlineCalendarResources.url(forResource: "OfflineCalendar", withExtension: "")!
        
        self.updater = CalendarBundleUpdater(configuration: self.configuration,
                                             storage: TestCalendarBundleStorage(cacheURL: storageConfiguration.cacheFolderURL,
                                                                                bundleURL: bundleURL,
                                                                                bundleInitializer: TestCalendarBundleInitializer(),
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

        NotificationCenter.default.post(name: CalendarBundleUpdater.Constants.updateNotificationName, object: nil)
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

        NotificationCenter.default.post(name: CalendarBundleUpdater.Constants.updateNotificationName, object: nil)
        self.wait(for: [expectation], timeout: 1)
    }
}

private final class TestCalendarBundleInitializer: ResourceBundleInitializer {
    let newVersionManifestFileName = "manifest.json"
    func initializeManifest(from data: Data) throws -> CalendarManifest {
        return CalendarManifest(info: .init(cache: .init(files: []),
                                            version: "10.2.3",
                                            prefetch: [ .init(name: "manifest", url: "badschema://myremoteurl", period: 10)]),
                                localization: CalendarManifest.Localization(title: .init(ru: "title_ru", en: "title_en"),
                                                                            company: .init(ru: "company_ru", en: "company_en"),
                                                                            service: .init(ru: "service_ru", en: "service_en"),
                                                                            firstRenderError: .init(ru: "firstRenderError_ru", en: "firstRenderError_en"),
                                                                            noBootData: .init(ru: "noBootData_ru", en: "noBootData_en")),
                                startURL: "https://calendar.yandex.ru/manifest",
                                indexPageURL: URL(string: "https://calendar.yandex.ru/manifest")!
        )
    }

    func initializeResourceBundle(from url: URL) throws -> CalendarBundle {
        return CalendarBundle(manifest: try! self.initializeManifest(from: Data(capacity: 1)),
                              manifestConfiguration: TestCalendarManifestConfiguration(),
                              manifestLocalURL: nil,
                              startPageURL: URL(string: "https://calendar.yandex.ru/manifest")!,
                              staticFilesMapping: [:])
    }
}

private final class TestCalendarBundleStorage: MayaBundleStorage<CalendarBundle> {}

private final class TestBundleStorageConfiguration: MayaBundleStorageConfiguration {
    var folderName: String = "OfflineCalendar"
    
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

private class TestLastTimeUpdater: MayaBundleLastTimeUpdater {
    var lastUpdateTime = Date()
}

private struct TestCalendarManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}

internal func YOXCTFail(_ message: String, file: StaticString = #file, line: UInt = #line) -> Never {
    XCTFail(message, file: file, line: line)
    fatalError("unreachable")
}
