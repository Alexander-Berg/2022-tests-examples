//
//  HTTPDynamicStubs.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 25.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

enum HTTPMethod {
    case GET
    case POST
    case PUT
    case DELETE
    case PATCH
}

final class HTTPDynamicStubs {
    init() {
        self.server = HttpServer()
        self.server.listenAddressIPv4 = Consts.address
    }

    func setUp() {
        do {
            try self.server.start(Consts.port, forceIPv4: true, priority: .userInitiated)
        }
        catch {
            XCTFail("Couldn't start HTTP server with stubs.\nReason: \(error)")
        }
    }

    func tearDown() {
        self.server.stop()
    }

    func register(
        method: HTTPMethod,
        path: String,
        middleware: MiddlewareProtocol,
        context: MiddlewareContext = .init()
    ) {
        let handler: ((HttpRequest) -> HttpResponse) = { request in
            var response: HttpResponse?

            let handlerContext = MiddlewareContext(context)
            middleware.handler(
                request: request,
                response: &response,
                context: handlerContext
            )

            let result: HttpResponse
            if let res = response {
                result = res
            }
            else {
                XCTFail("No response provided for \(method) \(path)")
                result = HttpResponse.notFound
            }

            return result
        }

        switch method {
            case .GET:
                self.server.GET[path] = handler
            case .POST:
                self.server.POST[path] = handler
            case .PUT:
                self.server.PUT[path] = handler
            case .DELETE:
                self.server.DELETE[path] = handler
            case .PATCH:
                self.server.PATCH[path] = handler
        }
    }

    func registerWebSocket(path: String, onConnect: @escaping (WebSocketSession) -> Void) {
        self.server[path] = websocket(connected: onConnect)
    }

    // MARK: Private

    private enum Consts {
        static let address: String = "127.0.0.1"
        static let port: in_port_t = 8080
    }

    private let server: HttpServer
}

extension HTTPDynamicStubs {
    func register(
        method: HTTPMethod,
        path: String,
        filename: String,
        requestTime: TimeInterval = 0
    ) {
        self.register(
            method: method,
            path: path,
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(requestTime),
                    .respondWith(.ok(.contentsOfJSON(filename))),
                ])
                .build()
        )
    }
}

// Deprecated
extension HTTPDynamicStubs {
    func setupStub(remotePath: String, filename: String, method: HTTPMethod = .GET, requestTime: TimeInterval = 0.0) {
        self.register(
            method: method,
            path: remotePath,
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(requestTime),
                    .respondWith(.ok(.contentsOfJSON(filename))),
                ])
                .build()
        )
    }
}
