//
//  Created by Timur Turaev on 06.08.2020.
//  Copyright © 2020 Timur Turaev. All rights reserved.
//

import Foundation
import XCTest
import WebKit
@testable import TurboApp

internal final class TurboAppWKURLSchemeHandlerTest: XCTestCase {
    private var logger: TestLogger!
    private var handler: TurboAppWKURLSchemeHandler!
    private var environment: TestTurboAppWKURLSchemeHandlerEnvironment!

    private enum Constants {
        static let normalStaticRemoteURL = URL(string: "https://file.ru/static")!
        static let nonExistedStaticRemoteURL = URL(string: "https://file.ru/missingStaticFile")!
        static let normalDynamicRemoteURL = URL(string: "deadbeef://file.ru/dynamic")!
        static let nonExistedDynamicRemoteURL = URL(string: "deadbeef://file.ru/missingDynamicFile")!

        static let remoteServerData = [
            Self.normalDynamicRemoteURL.settingURLScheme(to: "https"): "This is dynamic".data(using: .utf8)!
        ]

        static let testLocalFileURL = Bundle(for: TurboAppWKURLSchemeHandlerTest.self).url(forResource: "test", withExtension: "txt")!

        static let localResourcesData = [
            Self.normalStaticRemoteURL: try? Data(contentsOf: Self.testLocalFileURL),
            Self.nonExistedStaticRemoteURL: nil
        ]
    }

    override func setUp() {
        URLProtocolMock.testData = Constants.remoteServerData

        self.logger = TestLogger()
        self.environment = TestTurboAppWKURLSchemeHandlerEnvironment()
        self.environment.remoteToLocalMapper = Constants.localResourcesData

        let sessionConfiguration = URLSessionConfiguration.default
        sessionConfiguration.protocolClasses?.insert(URLProtocolMock.self, at: 0)
        self.handler = TurboAppWKURLSchemeHandler(environment: self.environment,
                                                  logger: self.logger,
                                                  authChallengeDisposition: .default,
                                                  dynamicResourcesLoadingSessionConfiguration: sessionConfiguration)

        super.setUp()
    }

    override func tearDown() {
        URLProtocolMock.reset()
        super.tearDown()
    }

    func testDeallocation() {
        weak var weakSessionDelegate: URLSessionDelegate?
        weak var weakHandler: TurboAppWKURLSchemeHandler?

        autoreleasepool {
            let sessionConfiguration = URLSessionConfiguration.default
            sessionConfiguration.protocolClasses?.insert(URLProtocolMock.self, at: 0)
            var handler: TurboAppWKURLSchemeHandler? = TurboAppWKURLSchemeHandler(environment: self.environment,
                                                                                  logger: self.logger,
                                                                                  authChallengeDisposition: .default,
                                                                                  dynamicResourcesLoadingSessionConfiguration: sessionConfiguration)
            weakSessionDelegate = handler?.dynamicResourcesLoadingSessionDelegate
            weakHandler = handler

            let task = TestWKURLSchemeTask(request: URLRequest(url: Constants.normalStaticRemoteURL))
            handler?.startWKURLSchemeTask(task)

            XCTAssertEqual(task.states, [.receiveResponse, .receiveData(try! String(contentsOf: Constants.testLocalFileURL).count), .finish])
            XCTAssertEqual(URLProtocolMock.requests, [])

            handler = nil
        }

        XCTAssertNil(weakHandler)
        self.wait(for: { weakSessionDelegate == nil }, timeout: 1.0)
        XCTAssertNil(weakSessionDelegate)
    }

    func testInterceptionNormalStaticFile() {
        let task = TestWKURLSchemeTask(request: URLRequest(url: Constants.normalStaticRemoteURL))
        self.handler.startWKURLSchemeTask(task)

        XCTAssertEqual(task.states, [.receiveResponse, .receiveData(try! String(contentsOf: Constants.testLocalFileURL).count), .finish])
        XCTAssertEqual(URLProtocolMock.requests, [])
        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testInterceptionNonExistingStaticFile() {
        let expectation = XCTestExpectation(description: #function)
        let request = URLRequest(url: Constants.nonExistedStaticRemoteURL)
        let task = TestWKURLSchemeTask(request: request)
        task.completion = {
            expectation.fulfill()
        }
        self.handler.startWKURLSchemeTask(task)
        self.wait(for: [expectation], timeout: 1)

        XCTAssertEqual(task.states, [.failed])
        XCTAssertEqual(URLProtocolMock.requests, [request.redactedRequest])
        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testInterceptionNormalDynamicFile() {
        let expectation = XCTestExpectation(description: #function)
        let request = URLRequest(url: Constants.normalDynamicRemoteURL)
        let httpsRequest = request.redactedRequest
        let task = TestWKURLSchemeTask(request: request)
        task.completion = {
            expectation.fulfill()
        }
        self.handler.startWKURLSchemeTask(task)

        self.wait(for: [expectation], timeout: 1)

        XCTAssertEqual(task.states, [.receiveResponse, .receiveData(Constants.remoteServerData[httpsRequest.url!]!.count), .finish])
        XCTAssertEqual(URLProtocolMock.requests, [httpsRequest])
        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testInterceptionNonExistingDynamicFile() {
        let expectation = XCTestExpectation(description: #function)
        let request = URLRequest(url: Constants.nonExistedDynamicRemoteURL)
        let task = TestWKURLSchemeTask(request: request)
        task.completion = {
            expectation.fulfill()
        }
        self.handler.startWKURLSchemeTask(task)

        self.wait(for: [expectation], timeout: 1)

        XCTAssertEqual(task.states, [.failed])
        XCTAssertEqual(URLProtocolMock.requests, [request.redactedRequest])
        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testInterceptionNormalDynamicFileServerSendNoResponse() {
        URLProtocolMock.sendResponse = false
        let expectation = XCTestExpectation(description: #function)
        let request = URLRequest(url: Constants.normalDynamicRemoteURL)
        let task = TestWKURLSchemeTask(request: request)
        task.completion = {
            expectation.fulfill()
        }
        self.handler.startWKURLSchemeTask(task)

        self.wait(for: [expectation], timeout: 1)

        XCTAssertEqual(task.states, [.failed])
        XCTAssertEqual(URLProtocolMock.requests, [request.redactedRequest])
        XCTAssertEqual(self.logger.infos.count, 1)
        XCTAssertEqual(self.logger.errors.count, 1)
    }

    func testInterceptionNormalDynamicFileServerSendNoData() {
        URLProtocolMock.sendData = false
        let expectation = XCTestExpectation(description: #function)
        let request = URLRequest(url: Constants.normalDynamicRemoteURL)
        let task = TestWKURLSchemeTask(request: request)
        task.completion = {
            expectation.fulfill()
        }
        self.handler.startWKURLSchemeTask(task)

        self.wait(for: [expectation], timeout: 1)

        XCTAssertEqual(task.states, [.receiveResponse, .receiveData(0), .finish])
        XCTAssertEqual(URLProtocolMock.requests, [request.redactedRequest])
        XCTAssertEqual(self.logger.infos.count, 2)
        XCTAssertEqual(self.logger.errors.count, 0)
    }

    func testStartAndStopInterception() {
        let request = URLRequest(url: Constants.normalDynamicRemoteURL)
        let task = TestWKURLSchemeTask(request: request)
        self.handler.startWKURLSchemeTask(task)
        self.handler.stopWKURLSchemeTask(task)

        RunLoop.current.run(until: Date().addingTimeInterval(0.3))

        XCTAssertEqual(task.states, [])
        XCTAssertEqual(URLProtocolMock.requests, [request.redactedRequest])
        XCTAssertEqual(self.logger.infos.count, 3)
        XCTAssertEqual(self.logger.errors.count, 0)
    }
}

private final class URLProtocolMock: URLProtocol {
    static var sendData = true
    static var sendResponse = true
    static var requests: [URLRequest] = []
    static var testData: [URL: Data] = [:]
    static var urlsWithError: [URL] = []

    static func reset() {
        self.sendData = true
        self.sendResponse = true
        self.requests.removeAll()
        self.testData.removeAll()
        self.urlsWithError.removeAll()
    }

    override class func canInit(with request: URLRequest) -> Bool {
        return true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        Self.requests.append(self.request)
        defer {
            self.client?.urlProtocolDidFinishLoading(self)
        }
        guard let url = self.request.url else {
            return
        }

        if let data = Self.testData[url] {
            if Self.sendResponse {
                self.client?.urlProtocol(self,
                                         didReceive: URLResponse(url: url, mimeType: "octet/stream", expectedContentLength: data.count, textEncodingName: nil),
                                         cacheStoragePolicy: .allowed)
            }
            if Self.sendData {
                self.client?.urlProtocol(self, didLoad: data)
            }
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

private final class TestWKURLSchemeTask: NSObject, WKURLSchemeTask {
    enum State: Equatable {
        case failed
        case receiveResponse
        case receiveData(Int)
        case finish
    }
    var completion: (() -> Void)?
    let request: URLRequest
    private(set) var states: [State] = []

    init(request: URLRequest) {
        self.request = request
    }

    func didReceive(_ response: URLResponse) {
        self.states.append(.receiveResponse)
    }

    func didReceive(_ data: Data) {
        self.states.append(.receiveData(data.count))
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

private struct TestTurboAppWKURLSchemeHandlerEnvironment: TurboAppWKURLSchemeHandlerEnvironment {
    var remoteToLocalMapper: [URL: Data?] = [:]

    func localDataForRemoteResourceAt(remoteURL: URL) -> Data? {
        guard let local = self.remoteToLocalMapper[remoteURL] else {
            return nil
        }
        return local
    }

    func expectedStaticMimeTypeForRemoteURL(_ url: URL) -> String {
        return "text/html"
    }
}

private extension URLRequest {
    var redactedRequest: Self {
        var redactedRequest = self.settingURLScheme(to: "https")
        redactedRequest.httpShouldHandleCookies = false
        return redactedRequest
    }

    func settingURLScheme(to newScheme: String) -> Self {
        var newRequest = self
        newRequest.url = self.url?.settingURLScheme(to: "https")
        return newRequest
    }
}
