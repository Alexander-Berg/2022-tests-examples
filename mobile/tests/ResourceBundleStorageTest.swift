//
//  Created by Timur Turaev on 28.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
@testable import TurboApp

internal final class ResourceBundleStorageTest: XCTestCase {
    private var storage: TestStorage!

    private var downloadedTempFolder: URL!
    private var cache: URL!
    private var bundle: URL!

    private func setupStorageEnvironment() {
        let fileManager = FileManager.default

        self.cache = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleStorageTest_Cache", isDirectory: true)
        fileManager.recreateDirectory(at: self.cache)

        let downloadedBundleURL = self.cache.appendingPathComponent(ResourceBundleStorageConstants.newVersionFolderName, isDirectory: true)
        fileManager.recreateDirectory(at: downloadedBundleURL)
        (1...3).forEach {
            try? "Downloaded.\($0)".data(using: .utf8)?.write(to: downloadedBundleURL.appendingPathComponent("\($0).txt"))
        }

        let currentBundleURL = self.cache.appendingPathComponent(ResourceBundleStorageConstants.currentVersionFolderName, isDirectory: true)
        fileManager.recreateDirectory(at: currentBundleURL)
        (1...3).forEach {
            try? "Current.\($0)".data(using: .utf8)?.write(to: currentBundleURL.appendingPathComponent("\($0).txt"))
        }

        self.bundle = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleStorageTest_Bundle", isDirectory: true)
        fileManager.recreateDirectory(at: self.bundle)
        (1...3).forEach {
            try? "Bundled.\($0)".data(using: .utf8)?.write(to: self.bundle.appendingPathComponent("\($0).txt"))
        }

        self.downloadedTempFolder = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleStorageTest_DownloadedTemp",
                                                                                                  isDirectory: true)
        fileManager.recreateDirectory(at: self.downloadedTempFolder)
        (1...3).forEach {
            try? "DownloadedTemp.\($0)".data(using: .utf8)?.write(to: self.downloadedTempFolder.appendingPathComponent("\($0).txt"))
        }
    }

    override func setUp() {
        super.setUp()
        self.setupStorageEnvironment()
        self.storage = TestStorage(cacheURL: self.cache, bundleURL: self.bundle)
    }

    func testForceUsingBundledVersion() {
        let fileManager = FileManager.default
        self.storage = TestStorage(cacheURL: self.cache, bundleURL: self.bundle, forceUseBundledManifest: true)

        let fetchExpectation = XCTestExpectation(description: #function)
        self.storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Bundled.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)

        let subfolders = (try? fileManager.contentsOfDirectory(at: self.cache, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(subfolders.count, 2)
        XCTAssertEqual(subfolders.map(\.lastPathComponent).sorted(), [ResourceBundleStorageConstants.currentVersionFolderName,
                                                                      ResourceBundleStorageConstants.newVersionFolderName])

        let directoryContent = (try? fileManager.contentsOfDirectory(at: self.storage.currentVersionBundleURL, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(directoryContent.count, 3)

        directoryContent.forEach { fileURL in
            let fileContent = (try? String(contentsOf: fileURL)) ?? ""
            XCTAssertEqual(fileContent, "Bundled.\(fileURL.deletingPathExtension().lastPathComponent)")
        }
    }

    func testFetchingDownloadedVersonOnStart() {
        let fileManager = FileManager.default

        let fetchExpectation = XCTestExpectation(description: #function)
        self.storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Downloaded.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)

        let subfolders = (try? fileManager.contentsOfDirectory(at: self.cache, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(subfolders.count, 1)
        XCTAssertEqual(subfolders.map(\.lastPathComponent).sorted(), [ResourceBundleStorageConstants.currentVersionFolderName])

        let directoryContent = (try? fileManager.contentsOfDirectory(at: self.storage.currentVersionBundleURL, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(directoryContent.count, 3)

        directoryContent.forEach { fileURL in
            let fileContent = (try? String(contentsOf: fileURL)) ?? ""
            XCTAssertEqual(fileContent, "Downloaded.\(fileURL.deletingPathExtension().lastPathComponent)")
        }
    }

    func testFetchingCurrentVersonOnStartIfNoDownloadedVersion() {
        let fileManager = FileManager.default

        // remove Downloaded directory
        fileManager.yo_removeItem(at: self.cache.appendingPathComponent(ResourceBundleStorageConstants.newVersionFolderName, isDirectory: true))

        let fetchExpectation = XCTestExpectation(description: #function)
        self.storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Current.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)

        let subfolders = (try? fileManager.contentsOfDirectory(at: self.cache, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(subfolders.count, 1)
        XCTAssertEqual(subfolders.map(\.lastPathComponent).sorted(), [ResourceBundleStorageConstants.currentVersionFolderName])

        let directoryContent = (try? fileManager.contentsOfDirectory(at: self.storage.currentVersionBundleURL, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(directoryContent.count, 3)

        directoryContent.forEach { fileURL in
            let fileContent = (try? String(contentsOf: fileURL)) ?? ""
            XCTAssertEqual(fileContent, "Current.\(fileURL.deletingPathExtension().lastPathComponent)")
        }
    }

    func testInitBundledVersonOnStartIfNoDownloadedAndCachedVersion() {
        let fileManager = FileManager.default

        // remove Downloaded directory and Current directory
        fileManager.yo_removeItem(at: self.cache.appendingPathComponent(ResourceBundleStorageConstants.newVersionFolderName, isDirectory: true))
        fileManager.yo_removeItem(at: self.cache.appendingPathComponent(ResourceBundleStorageConstants.currentVersionFolderName, isDirectory: true))

        let fetchExpectation = XCTestExpectation(description: #function)
        self.storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Bundled.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)

        let subfolders = (try? fileManager.contentsOfDirectory(at: self.cache, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(subfolders.count, 1)
        XCTAssertEqual(subfolders.map(\.lastPathComponent).sorted(), [ResourceBundleStorageConstants.currentVersionFolderName])

        let directoryContent = (try? fileManager.contentsOfDirectory(at: self.storage.currentVersionBundleURL, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(directoryContent.count, 3)

        directoryContent.forEach { fileURL in
            let fileContent = (try? String(contentsOf: fileURL)) ?? ""
            XCTAssertEqual(fileContent, "Bundled.\(fileURL.deletingPathExtension().lastPathComponent)")
        }
    }

    func testSavingNewResourceBundle() {
        let fileManager = FileManager.default
        let expectation = XCTestExpectation(description: "saveNewResourceBundle")
        self.storage.saveNewResourceBundle(at: self.downloadedTempFolder, completionQueue: DispatchQueue.main) {
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 5)

        let subfolders = (try? fileManager.contentsOfDirectory(at: self.cache, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(subfolders.count, 2)
        XCTAssertEqual(subfolders.map(\.lastPathComponent).sorted(), [ResourceBundleStorageConstants.currentVersionFolderName,
                                                                      ResourceBundleStorageConstants.newVersionFolderName])

        let directoryContent = (try? fileManager.contentsOfDirectory(at: self.storage.newVersionBundleURL, includingPropertiesForKeys: nil, options: [])) ?? []
        XCTAssertEqual(directoryContent.count, 3)

        directoryContent.forEach { fileURL in
            let fileContent = (try? String(contentsOf: fileURL)) ?? ""
            XCTAssertEqual(fileContent, "DownloadedTemp.\(fileURL.deletingPathExtension().lastPathComponent)")
        }
    }

    func testFetchingErrorProneDownloadedBundle() {
        let storage = TestStorage(cacheURL: self.cache,
                                  bundleURL: self.bundle,
                                  bundleInitializer: TestErrorInitializer(errorProneURLs: [self.storage.newVersionBundleURL],
                                                                          errorProneCurrentURL: nil),
                                  forceUseBundledManifest: false)

        let fetchExpectation = XCTestExpectation(description: #function)
        storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Current.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)
    }

    func testFetchingErrorProneDownloadedAndCachedBundle() {
        let storage = TestStorage(cacheURL: self.cache,
                                  bundleURL: self.bundle,
                                  bundleInitializer: TestErrorInitializer(errorProneURLs: [self.storage.newVersionBundleURL],
                                                                          errorProneCurrentURL: self.storage.currentVersionBundleURL),
                                  forceUseBundledManifest: false)

        let fetchExpectation = XCTestExpectation(description: #function)
        storage.fetchResourceBundle(completionQueue: .main) { result in
            let resourceBundle = try! XCTUnwrap(result.toOptional())
            XCTAssertEqual(String(data: resourceBundle.data!, encoding: .utf8)!, "Bundled.1")
            fetchExpectation.fulfill()
        }
        self.wait(for: [fetchExpectation], timeout: 1)
    }
}

private struct TestManifest: Manifest {
    let version: String
    let data: Data?

    var remoteURL: URL? {
        return URL(string: "https://calendar.yandex.ru/manifest")
    }

    func applyingNewManifest(_ newManifest: TestManifest) -> TestManifest {
        return newManifest
    }
}

private struct TestResourceBundle: ResourceBundle {
    let manifestLocalURL: LocalFileURL? = nil
    let manifestVersion: String
    let data: Data?

    var manifest: TestManifest {
        return TestManifest(version: self.manifestVersion, data: self.data)
    }

    var rawManifest: TestManifest {
        return self.manifest
    }
}

private final class RegularInitializer: ResourceBundleInitializer {
    let newVersionManifestFileName = "manifest.json"

    enum Error: Swift.Error {
        case initializationBundleFailed(URL)
    }

    func initializeManifest(from data: Data) throws -> TestManifest {
        return TestManifest(version: "0.2.3", data: data)
    }

    func initializeResourceBundle(from url: URL) throws -> TestResourceBundle {
        let folderContent = (try? FileManager.default.contentsOfDirectory(atPath: url.path)) ?? []

        guard !folderContent.isEmpty else {
            throw Error.initializationBundleFailed(url)
        }

        let content = try! String(contentsOf: url.appendingPathComponent("1.txt"))
        return TestResourceBundle(manifestVersion: "0.2.3-\(url.lastPathComponent)", data: content.data(using: .utf8))
    }
}

private final class TestStorage: ResourceBundleStorage<TestResourceBundle> {
    convenience init(cacheURL: URL, bundleURL: URL, forceUseBundledManifest: Bool = false) {
        self.init(cacheURL: cacheURL,
                  bundleURL: bundleURL,
                  bundleInitializer: RegularInitializer(),
                  forceUseBundledManifest: forceUseBundledManifest)
    }
}

private struct TestErrorInitializer: ResourceBundleInitializer {
    let newVersionManifestFileName = "manifest.json"

    let errorProneURLs: [URL]
    let errorProneCurrentURL: URL?
    
    enum TestError: Swift.Error {
        case initializeResourceBundle(url: URL)
    }

    func initializeManifest(from data: Data) throws -> TestManifest {
        fatalError("Unreachable")
    }

    func initializeResourceBundle(from url: URL) throws -> TestResourceBundle {
        if self.errorProneURLs.contains(url) {
            throw TestError.initializeResourceBundle(url: url)
        }

        let content = try! String(contentsOf: url.appendingPathComponent("1.txt"))
        if self.errorProneCurrentURL == url && content.lowercased().starts(with: "current") {
            throw TestError.initializeResourceBundle(url: url)
        }

        return TestResourceBundle(manifestVersion: "Error-0.2.3", data: content.data(using: .utf8))
    }
}
