//
//  TestHTTPClientBuilder.swift
//  TestUtils
//
//  Created by Timur Turaev on 02.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import Foundation
import NetworkLayer

public enum TestHTTPClientKind {
    case realAndCapture(withTabs: Bool)
    case playFrom(Bundle?)
}

public final class TestHTTPClientBuilder {
    private let fixtureName: String
    private let token: String
    private let baseURL: URL
    private let networkOperationDelay: Int

    public init(fixtureName: String,
                token: String = "DO_NOT_COMMIT_IT",
                baseURL: URL = URL(string: "https://mail.yandex.ru/api/mobile")!,
                networkOperationDelay: Int = 0) {
        self.fixtureName = fixtureName
        self.token = token
        self.baseURL = baseURL
        self.networkOperationDelay = networkOperationDelay
    }

    public func buildTestClient(kind: TestHTTPClientKind) -> HTTPClientProtocol {
        switch kind {
        case .realAndCapture(let withTabs):
            let requestBuilder = HTTPClientRequestHandleBuilderCapture(fixtureName: self.fixtureName,
                                                                       login: "login",
                                                                       credentials: YOURLCredentials(token: self.token),
                                                                       httpClientVaryingParameters: TestParameters(baseURL: self.baseURL, tabs: withTabs))
            return YOHTTPClient(requestBuilder: requestBuilder)
        case .playFrom(let bundle):
            let requestBuilder = HTTPClientRequestHandleBuilderPlayer(bundle: bundle,
                                                                      fixtureName: self.fixtureName,
                                                                      enableRequestValidate: true,
                                                                      networkOperationDelay: self.networkOperationDelay)
            return YOHTTPClient(requestBuilder: requestBuilder)
        }
    }

    public func buildRealClient(withTabs: Bool = false) -> HTTPClientProtocol {
        let requestBuilder = HTTPClientAuthorizedRequestHandleBuilder(login: "login",
                                                                      uuid: "deadbeef",
                                                                      credentials: YOURLCredentials(token: self.token),
                                                                      httpClientVaryingParameters: TestParameters(baseURL: self.baseURL, tabs: withTabs),
                                                                      sharedContainerIdentifier: nil,
                                                                      taskDidFinishCollectionMetricsBlock: nil,
                                                                      authChallengeDisposition: AuthChallengeDisposition.allowEverything.authChallengeDispositionCompat)
        return YOHTTPClient(requestBuilder: requestBuilder)
    }
}

private final class TestParameters: HTTPClientVaryingParameters {
    private let baseURL: URL
    private let tabsAvailableForLogin: Bool
    fileprivate init(baseURL: URL, tabs: Bool) {
        self.baseURL = baseURL
        self.tabsAvailableForLogin = tabs
    }

    func baseURLFor(_ login: String?) -> URL {
        return self.baseURL
    }

    func tabsAvailableForLogin(_ login: String?) -> Bool {
        return self.tabsAvailableForLogin
    }

    var client: String = ""

    var appState: String? = ""

    var uuid: String? = "deadbeef"

    var userAgent: String = ""
}
