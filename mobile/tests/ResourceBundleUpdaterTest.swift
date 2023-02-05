//
//  Created by Timur Turaev on 28.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import Foundation

import XCTest
@testable import TurboApp

internal final class ResourceBundleUpdaterTest: XCTestCase {
    private var updater: TestUpdater!
    private var storage: TestStorage!
    private var loader: TestLoader!
    private var configuration: TestUpdaterConfiguration!
    private var initializer: TestInitializer!

    private var bundleURL: URL!
    private var cacheURL: URL!

    override func setUp() {
        super.setUp()

        self.bundleURL = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleUpdaterTest_Bundle", isDirectory: true)
        FileManager.default.recreateDirectory(at: self.bundleURL)
        (1...3).forEach {
            try? "Bundle.\($0)".data(using: .utf8)?.write(to: self.bundleURL.appendingPathComponent("\($0).txt"))
        }

        self.cacheURL = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleUpdaterTest_Cache", isDirectory: true)
        FileManager.default.recreateDirectory(at: self.cacheURL)

        self.initializer = TestInitializer(manifestLocalURL: self.cacheURL.appendingPathComponent("manifestLocalURL.txt"))

        self.storage = TestStorage(cacheURL: self.cacheURL,
                                   bundleURL: self.bundleURL,
                                   bundleInitializer: self.initializer,
                                   forceUseBundledManifest: false)
        self.loader = TestLoader()
        self.configuration = TestUpdaterConfiguration()
        self.updater = TestUpdater(configuration: self.configuration, storage: self.storage, loader: self.loader)
    }

    override func tearDown() {
        super.tearDown()

        self.updater = nil
    }

    func testNormalUpdating() {
        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            XCTAssertNotNil(result.toOptional())
            XCTAssertEqual(self.configuration.newTimeSetterCount, 1)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 100)

        XCTAssertTrue(self.storage.saveNewManifestCalled)
    }

    func testNormalFrequentlyUpdating() {
        let expectation = XCTestExpectation(description: #function)
        expectation.assertForOverFulfill = true
        expectation.expectedFulfillmentCount = 3

        var counter = 0
        self.updater.updatingCompletedBlock = { result in
            counter += 1

            if counter < 3 {
                guard case .failure(.updatingAlreadyInProgress) = result else {
                    YOXCTFail("Wrong result: \(result)")
                }
                XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            } else {
                XCTAssertEqual(self.configuration.newTimeSetterCount, 1)
                XCTAssertNotNil(result.toOptional())
            }
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
        XCTAssertEqual(counter, 3)
    }

    func testUpdatingWithoutResourceBundle() {
        FileManager.default.recreateDirectory(at: self.bundleURL)
        FileManager.default.recreateDirectory(at: self.cacheURL)

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.failedToFetchResourceBundleFromStorage) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingTooFast() {
        self.configuration.updatePeriodIsOK = false

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.updatingTooFrequent) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingWithoutRemoteURL() {
        self.initializer.remoteURL = nil

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.noRemoteURLInResourceBundle) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingWithoutRemoteData() {
        self.loader.hasRemoteData = false

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.downloadingRemoteManifestFailed) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingWithMalformedRemoteManifest() {
        self.initializer.canInitializeManifestFromData = false

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.remoteManifestParsingError) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 0)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingWhenRemoteVersionIsNotNewerThanLocal() {
        self.initializer.remoteVersion = "1.2.3"

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.remoteManifestHasSameVersion("1.2.3")) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 1)
            expectation.fulfill()
        }

        let newManifestLocalURL = self.cacheURL.appendingPathComponent("manifestLocalURL.txt")
        XCTAssertFalse(FileManager.default.fileExists(atPath: newManifestLocalURL.path))
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)

        XCTAssertTrue(self.storage.saveNewManifestCalled)

        self.wait(for: { () -> Bool in
            FileManager.default.fileExists(atPath: newManifestLocalURL.path)
        }, timeout: 1)

        let content = try? String(contentsOf: newManifestLocalURL)
        XCTAssertNotNil(content)
        XCTAssertFalse(content!.isEmpty)
    }

    func testUpdatingWhenUnableToDownloadRemoteFiles() {
        self.loader.remoteFilesDownloadingIsOK = false

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            guard case .failure(.remoteManifestStaticFilesNotLoaded) = result else {
                YOXCTFail("Wrong result: \(result)")
            }
            XCTAssertEqual(self.configuration.newTimeSetterCount, 1)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)

        self.wait(for: [expectation], timeout: 1)
    }

    func testUpdatingFromCustomRemoteManifest() {
        self.initializer.remoteVersion = "1.0.0"
        let customRemoteURL = "https://calendar.yandex.ru/"

        self.updater = TestUpdater(configuration: self.configuration,
                                   storage: self.storage,
                                   loader: self.loader,
                                   customRemoteURL: URL(string: customRemoteURL)!)

        let expectation = XCTestExpectation(description: #function)
        self.updater.updatingCompletedBlock = { result in
            XCTAssertNotNil(result.toOptional())

            XCTAssertEqual(result.toOptional()!.version, "1.0.0")
            XCTAssertEqual(result.toOptional()!.data, customRemoteURL.data(using: .utf8)!)
            XCTAssertEqual(self.configuration.newTimeSetterCount, 1)
            expectation.fulfill()
        }
        NotificationCenter.default.post(name: TestUpdater.Constants.notification, object: nil)
        self.wait(for: [expectation], timeout: 1)
    }
}

// MARK: - Test Data

private struct TestManifest: Manifest {
    let data: Data?
    let remoteURL: URL?
    let version: String

    func applyingNewManifest(_ newManifest: TestManifest) -> TestManifest {
        return newManifest
    }
}

private struct TestResourceBundle: ResourceBundle {
    let data: Data?
    let remoteURL: URL?
    let version: String
    let manifestLocalURL: LocalFileURL?

    var manifest: TestManifest {
        return TestManifest(data: self.data, remoteURL: self.remoteURL, version: self.version)
    }

    var rawManifest: TestManifest {
        return self.manifest
    }
}

private final class TestInitializer: ResourceBundleInitializer {
    let manifestLocalURL: LocalFileURL
    let newVersionManifestFileName = "manifest.json"
    var canInitializeManifestFromData = true
    var remoteVersion = "1.3.4"
    var remoteURL: URL? = URL(string: "https://yandex.ru/")

    init(manifestLocalURL: LocalFileURL) {
        self.manifestLocalURL = manifestLocalURL
    }

    enum Error: Swift.Error {
        case initializationManifestFailed(Swift.Error)
        case initializationBundleFailed(URL)
    }

    func initializeManifest(from data: Data) throws -> TestManifest {
        guard self.canInitializeManifestFromData else {
            throw Error.initializationManifestFailed(CocoaError.error(.fileNoSuchFile))
        }
        return TestManifest(data: data,
                            remoteURL: self.remoteURL,
                            version: self.remoteVersion)
    }

    func initializeResourceBundle(from url: URL) throws -> TestResourceBundle {
        let folderContent = (try? FileManager.default.contentsOfDirectory(atPath: url.path)) ?? []

        guard !folderContent.isEmpty else {
            throw Error.initializationBundleFailed(url)
        }

        return TestResourceBundle(data: url.absoluteString.data(using: .utf8),
                                  remoteURL: self.remoteURL,
                                  version: "1.2.3",
                                  manifestLocalURL: self.manifestLocalURL)
    }
}

private class TestStorage: ResourceBundleStorage<TestResourceBundle> {
    var saveNewManifestCalled = false

    override func saveNewManifest(_ newManifest: TestManifest) {
        self.saveNewManifestCalled = true

        super.saveNewManifest(newManifest)
    }
}

private class TestUpdaterConfiguration: ResourceBundleUpdaterConfiguration {
    var newTimeSetterCount = 0
    var updatePeriodIsOK = true

    func checkResourceBundleUpdatePeriod(_ resourceBundle: TestResourceBundle) -> Bool {
        return self.updatePeriodIsOK
    }

    func setNewResourceBundleUpdateTime(_ newTime: Date) {
        self.newTimeSetterCount += 1
    }

    func remoteFileURLs(from remoteManifest: TestManifest,
                        considering resourceBundle: TestResourceBundle) -> RemoteFilesToDownload {
        return .empty
    }
}

private class TestUpdater: ResourceBundleUpdater<TestResourceBundle> {
    enum Constants {
        static let tempDirectoryName = "TestUpdaterTempDirectoryName"
        static let notification = Notification.Name("TestUpdaterNotification")
    }

    init(configuration: TestUpdaterConfiguration,
         storage: TestStorage,
         loader: ResourceBundleLoading,
         customRemoteURL: URL? = nil) {
        super.init(configuration: configuration,
                   storage: storage,
                   customManifestRemoteURL: customRemoteURL,
                   temporaryDirectoryNameForNewBundle: Constants.tempDirectoryName,
                   loader: loader,
                   notificationName: Constants.notification)
    }
}

private class TestLoader: ResourceBundleLoading {
    var hasRemoteData = true
    var remoteFilesDownloadingIsOK = true

    func downloadFileFrom(url: URL, loadingCompletionQueue: DispatchQueue, completionBlock: @escaping (Result<Data, ResourceBundleLoaderError>) -> Void) {
        loadingCompletionQueue.async {
            completionBlock(self.hasRemoteData
                ? .success(url.absoluteString.data(using: .utf8)!)
                : .failure(.loadingError(CocoaError.error(.fileNoSuchFile))))
        }
    }

    func downloadFilesFrom(remoteFilesToDownload: RemoteFilesToDownload,
                           destinationFolderURL: LocalFileURL,
                           loadingCompletionQueue: DispatchQueue,
                           completionBlock: @escaping ([RemoteURL: ResourceBundleLoaderError]) -> Void) {
        loadingCompletionQueue.async {
            completionBlock(self.remoteFilesDownloadingIsOK
                                ? [:]
                                : [URL(string: "https://a.a")!: .loadingError(CocoaError.error(.fileNoSuchFile))])
        }
    }
}

internal func YOXCTFail(_ message: String, file: StaticString = #file, line: UInt = #line) -> Never {
    XCTFail(message, file: file, line: line)
    fatalError("unreachable")
}
