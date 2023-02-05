// 
// TestUtils.swift 
// Authors:  Alexey A. Ushakov 
// Copyright © 2013-2017 Yandex. All rights reserved. 
// 

import XCTest
import TestUtils
@testable import YandexMobileMail

private final class HTTPClientWithAdditionalRequestHandling<T>: YOHTTPClient {
    private let additionalRequestHandler: (T) -> Void

    init(additionalRequestHandler: @escaping (T) -> Void, requestBuilder: YOHTTPClientRequestHandleBuilder) {
        self.additionalRequestHandler = additionalRequestHandler
        super.init(requestBuilder: requestBuilder)
    }
    
    fileprivate override func run(_ request: YORequest) -> YORequestHandle {
        if let typedRequest = request as? T {
            self.additionalRequestHandler(typedRequest)
        }
        return super.run(request)
    }
}

final class TestUtils {
    let login: String
    
    static func testBundle() -> Bundle {
        return Bundle(for: self)
    }

    init(login: String = "djdonkey@yandex.ru") {
        self.login = login
    }
    
    func httpClient(fixtureName: String, enableRequestFixtureValidate: Bool = false) -> YOHTTPClient {
        let builder = HTTPClientRequestHandleBuilderPlayer(bundle: Self.testBundle(),
                                                           fixtureName: fixtureName,
                                                           enableRequestValidate: enableRequestFixtureValidate,
                                                           networkOperationDelay: 0)
        return YOHTTPClient(requestBuilder: builder)
    }
    
    func httpClient<T>(fixtureName: String, additionalRequestHandler: @escaping (T) -> Void) -> YOHTTPClient {
        return HTTPClientWithAdditionalRequestHandling(additionalRequestHandler: additionalRequestHandler,
                                                       requestBuilder: HTTPClientRequestHandleBuilderPlayer(bundle: Self.testBundle(),
                                                                                                            fixtureName: fixtureName,
                                                                                                            enableRequestValidate: false,
                                                                                                            networkOperationDelay: 0))
    }
}
