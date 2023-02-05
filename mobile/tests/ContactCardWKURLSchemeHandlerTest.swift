//
//  Created by Timur Turaev on 07.08.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import XCTest
import Utils
import TurboApp
import WebKit
import TestUtils
@testable import ContactCard

internal final class ContactCarddWKURLSchemeHandlerTest: XCTestCase {
    private var logger: TestLogger!
    private var storage: ContactCardBundleStorage!

    override func setUp() {
        super.setUp()
        let configuration = TestConfiguration()
        FileManager.default.recreateDirectory(at: configuration.cacheFolderURL)
        self.storage = ContactCardBundleStorage(configuration: configuration)
        self.logger = TestLogger()
    }

    override func tearDown() {
        super.tearDown()
    }
    
    func testInterceptionStaticFilesFromContactCardBundle() {
        let bundle = self.contactCardBundle
        let user = User(login: "test",
                        email: "test@login.ru",
                        displayName: "Test Testov",
                        isCorporate: false,
                        uid: 123,
                        token: "test oauth token")

        let sessionConfiguration = URLSessionConfiguration.default
        sessionConfiguration.protocolClasses?.insert(URLProtocolMock.self, at: 0)
        let startPageRemoteURL = URL(string: "yo-turbo-app-contact-card://mail.yandex.ru")!
        let handler = ContactCardWKURLSchemeHandler(bundle: bundle,
                                                    startURL: startPageRemoteURL,
                                                    user: user,
                                                    contact: Contact(email: "test@yandex-team.ru",
                                                                     name: "Test",
                                                                     image: nil,
                                                                     monogram: "QQ",
                                                                     color: "FFFFFF"),
                                                    features: .init(showFiltersButton: true),
                                                    showVersion: true,
                                                    authChallengeDisposition: .default,
                                                    urlSessionConfiguration: sessionConfiguration,
                                                    networkStatusInfo: TestNetworkMonitor(),
                                                    logger: self.logger)

        bundle.staticFilesMapping.forEach { (remoteURL: URL, localURL: URL) in
            let task = TestWKURLSchemeTask(request: URLRequest(url: remoteURL))
            handler.startWKURLSchemeTask(task)
            XCTAssertEqual(task.states, [.receiveResponse, .receiveData(try! Data(contentsOf: localURL)), .finish])
            XCTAssertEqual(self.logger.infos.count, 1)
            XCTAssertEqual(self.logger.errors.count, 0)
            self.logger.infos.removeAll()
            self.logger.errors.removeAll()
        }

        let task = TestWKURLSchemeTask(request: URLRequest(url: startPageRemoteURL))
                handler.startWKURLSchemeTask(task)
                XCTAssertEqual(task.states.first, .receiveResponse)
                guard case let .receiveData(startPageData) = task.states.dropFirst().first! else {
                    XCTFail("second state should be receiveData with any size")
                    fatalError("unreachable")
                }
                XCTAssertEqual(task.states.last, .finish)
                XCTAssertEqual(task.states.count, 3)
                XCTAssertEqual(self.logger.infos.count, 1)
                XCTAssertEqual(self.logger.errors.count, 0)
                let startPageContent = String(data: startPageData, encoding: .utf8)!
                XCTAssertFalse(startPageContent.contains("{{"))
    }

    private var contactCardBundle: ContactCardBundle {
        let expectation = XCTestExpectation(description: #function)
        var bundle: ContactCardBundle?
        storage.fetchResourceBundle(completionQueue: DispatchQueue.main) { contactCardBundle in
            bundle = contactCardBundle.toOptional()
            expectation.fulfill()
        }
        self.wait(for: [expectation], timeout: 1)
        return bundle!
    }
}

private final class TestConfiguration: MayaBundleStorageConfiguration {
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

private struct TestContactCardManifestConfiguration: MayaManifestConfiguration {
    var language: Language {
        return .ru
    }
}

private final class URLProtocolMock: URLProtocol {
    override class func canInit(with request: URLRequest) -> Bool {
        fatalError("Try to execure reqeust \(request)")
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        // do nothing
    }

    override func stopLoading() {
        // do nothing
    }
}

private final class TestWKURLSchemeTask: NSObject, WKURLSchemeTask {
    enum State: Equatable {
        case failed
        case receiveResponse
        case receiveData(Data)
        case finish
    }
    var completion: (() -> Void)?
    let request: URLRequest
    private(set) var states: [State] = .empty

    init(request: URLRequest) {
        self.request = request
    }

    func didReceive(_ response: URLResponse) {
        self.states.append(.receiveResponse)
    }

    func didReceive(_ data: Data) {
        self.states.append(.receiveData(data))
    }

    func didFinish() {
        self.states.append(.finish)
        self.completion?()
    }

    func didFailWithError(_ error: Error) {
        self.states.append(.failed)
        self.completion?()
    }
}

private final class TestNetworkMonitor: NetworkStatusInfo {
    var status: NetworkStatus {
        return .reachable
    }
}
