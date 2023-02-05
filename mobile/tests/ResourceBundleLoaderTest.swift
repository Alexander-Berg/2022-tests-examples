//
//  TurboAppTests.swift
//  TurboAppTests
//
//  Created by Timur Turaev on 27.07.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
@testable import TurboApp

internal final class ResourceBundleLoaderTest: XCTestCase {
    private var loader: ResourceBundleLoader!

    private enum Constants {
        static let testData = [
            URL(string: "https://file.ru/1")!: "1".data(using: .utf8)!,
            URL(string: "https://file.ru/2")!: "2".data(using: .utf8)!,
            URL(string: "https://file.ru/")!: "This file should not be exist".data(using: .utf8)!
        ]
        static let nonTestURL = URL(string: "https://yandex.ru")!

        static let existingRemoteFileURL = URL(string: "https://file.ru/1")!

        static let completionQueueLabel = "ResourceBundleLoader_completionQueueLabel"
    }

    override func setUp() {
        super.setUp()

        URLProtocolMock.testData = Constants.testData
        let sessionConfiguration = URLSessionConfiguration.default
        sessionConfiguration.protocolClasses?.insert(URLProtocolMock.self, at: 0)

        self.loader = ResourceBundleLoader(authChallengeDisposition: .default, sessionConfiguration: sessionConfiguration)
    }

    override func tearDown() {
        super.tearDown()

        URLProtocolMock.urlsWithError.removeAll()
    }

    func testLoadingNormalFile() {
        let expectation = XCTestExpectation(description: "loading normal file")
        let url = Constants.testData.keys.first!
        self.loader.downloadFileFrom(url: url, loadingCompletionQueue: self.loadingCompletionQueue) { (result: Result<Data, ResourceBundleLoaderError>) in
            XCTAssertEqual(DispatchQueue.currentQueueLabel, Constants.completionQueueLabel)
            result.onValue {
                XCTAssertEqual($0, Constants.testData[url])
                expectation.fulfill()
            }
        }

        self.wait(for: [expectation], timeout: 1)
        XCTAssertEqual(URLProtocolMock.urlsWithError.count, 0)
    }

    func testLoadingNonTestFile() {
        let expectation = XCTestExpectation(description: "loading non-test file")
        self.loader.downloadFileFrom(url: Constants.nonTestURL, loadingCompletionQueue: self.loadingCompletionQueue) { (result: Result<Data, ResourceBundleLoaderError>) in
            XCTAssertEqual(DispatchQueue.currentQueueLabel, Constants.completionQueueLabel)
            result.onError {
                guard case .loadingError = $0 else {
                    XCTFail("Wrong loading error")
                    return
                }
                expectation.fulfill()
            }
        }

        self.wait(for: [expectation], timeout: 1)
        XCTAssertEqual(URLProtocolMock.urlsWithError.count, 1)
    }

    func testLoadingAndSavingNormalFiles() {
        let expectation = XCTestExpectation(description: "loading and saving normal files")

        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleLoaderTest", isDirectory: true)
        FileManager.default.recreateDirectory(at: folder)

        self.loader.downloadFilesFrom(remoteFilesToDownload: RemoteFilesToDownload(urlsToDownload: Array(Constants.testData.keys), existingFiles: [:]),
                                      destinationFolderURL: folder,
                                      loadingCompletionQueue: self.loadingCompletionQueue) { (nonLoadedFiles: [URL: ResourceBundleLoaderError]) in
            XCTAssertEqual(DispatchQueue.currentQueueLabel, Constants.completionQueueLabel)
            XCTAssertEqual(nonLoadedFiles.count, 1)

            if case let .writeDiskError(error) = nonLoadedFiles.values.first {
                XCTAssertEqual((error as NSError).domain, NSCocoaErrorDomain)
                XCTAssertEqual((error as NSError).code, NSFileWriteUnknownError)
            } else {
                XCTFail("Wrong loading error")
                return
            }

            let files = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil, options: [])) ?? []
            XCTAssertEqual(files.count, 2)

            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("1", isDirectory: false)), "1")
            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("2", isDirectory: false)), "2")

            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1)
        XCTAssertEqual(URLProtocolMock.urlsWithError.count, 0)
    }

    /////////////

    func testNotLoadingAlreadyDownloadedFiles() {
        let expectation = XCTestExpectation(description: "not loading already downloaded files")

        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleLoaderTest", isDirectory: true)
        FileManager.default.recreateDirectory(at: folder)

        let existingFileRemoteURL = Constants.existingRemoteFileURL
        let existingData = Constants.testData[Constants.existingRemoteFileURL]!
        let existingFileLocalURL = folder.appendingPathComponent("existingFile.dat", isDirectory: false)
        try! existingData.write(to: existingFileLocalURL)

        XCTAssertNotNil(try? String(contentsOf: existingFileLocalURL))

        self.loader.downloadFilesFrom(remoteFilesToDownload: RemoteFilesToDownload(urlsToDownload: Array(Constants.testData.keys),
                                                                                   existingFiles: [existingFileRemoteURL: existingFileLocalURL]),
                                      destinationFolderURL: folder,
                                      loadingCompletionQueue: self.loadingCompletionQueue) { (nonLoadedFiles: [URL: ResourceBundleLoaderError]) in
            XCTAssertEqual(DispatchQueue.currentQueueLabel, Constants.completionQueueLabel)
            XCTAssertEqual(nonLoadedFiles.count, 1)

            if case let .writeDiskError(error) = nonLoadedFiles.values.first {
                XCTAssertEqual((error as NSError).domain, NSCocoaErrorDomain)
                XCTAssertEqual((error as NSError).code, NSFileWriteUnknownError)
            } else {
                XCTFail("Wrong loading error")
                return
            }

            let files = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil, options: [])) ?? []
            XCTAssertEqual(files.count, 3) // existingFile.dat + 1 + 2

            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("1", isDirectory: false)), "1")
            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("2", isDirectory: false)), "2")

            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 500)
        XCTAssertEqual(URLProtocolMock.urlsWithError.count, 0)
    }

    func testLoadingAndSavingNormalAndNonTestFile() {
        let expectation = XCTestExpectation(description: "loading and saving normal and non-test files")

        let folder = FileManager.default.temporaryDirectory.appendingPathComponent("ResourceBundleLoaderTest", isDirectory: true)
        try? FileManager.default.removeItem(at: folder)
        try? FileManager.default.createDirectory(at: folder, withIntermediateDirectories: true, attributes: nil)

        self.loader.downloadFilesFrom(remoteFilesToDownload: RemoteFilesToDownload(urlsToDownload: Array(Constants.testData.keys) + [Constants.nonTestURL],
                                                                                   existingFiles: [:]),
                                      destinationFolderURL: folder,
                                      loadingCompletionQueue: self.loadingCompletionQueue) { (nonLoadedFiles: [URL: ResourceBundleLoaderError]) in
            XCTAssertEqual(DispatchQueue.currentQueueLabel, Constants.completionQueueLabel)
            XCTAssertEqual(nonLoadedFiles.count, 2)
            if case let .loadingError(error) = nonLoadedFiles[Constants.nonTestURL] {
                XCTAssertEqual((error as NSError).code, 404)
            } else {
                XCTFail("Wrong loading error")
                return
            }

            let files = (try? FileManager.default.contentsOfDirectory(at: folder, includingPropertiesForKeys: nil, options: [])) ?? []
            XCTAssertEqual(files.count, 2)

            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("1", isDirectory: false)), "1")
            XCTAssertEqual(try? String(contentsOf: folder.appendingPathComponent("2", isDirectory: false)), "2")

            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 1)
        XCTAssertEqual(URLProtocolMock.urlsWithError.count, 4) // 4 attempts to download Constants.nonTestURL
    }

    private var loadingCompletionQueue: DispatchQueue {
        return DispatchQueue(label: Constants.completionQueueLabel)
    }
}

private final class URLProtocolMock: URLProtocol {
    static var testData: [URL: Data] = [:]
    static var urlsWithError: [URL] = []

    override class func canInit(with request: URLRequest) -> Bool {
        return true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        defer {
            self.client?.urlProtocolDidFinishLoading(self)
        }
        guard let url = self.request.url else {
            return
        }

        if let data = Self.testData[url] {
            self.client?.urlProtocol(self, didLoad: data)
        } else {
            Self.urlsWithError.append(url)

            self.client?.urlProtocol(self, didFailWithError: NSError(domain: "ru.yandex.mail", code: 404, userInfo: [
                NSLocalizedDescriptionKey: "no test data for [\(self.request)]"
            ]))
        }
    }

    override func stopLoading() {
        // do nothing
    }
}

private extension DispatchQueue {
    static var currentQueueLabel: String? {
        return String(validatingUTF8: __dispatch_queue_get_label(nil))
    }
}
