//
//  Created by Alexey Aleshkov on 02/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import class Swifter.HttpRequest
import enum Swifter.HttpResponse

// Pure middlewares. No asserts here.

struct Middlewares {
    static func noop() -> NoopMiddleware {
        return .instance
    }

    static func requestTime(_ requestTime: @escaping @autoclosure () -> TimeInterval) -> RequestTimeMiddleware {
        return .init(requestTime: requestTime)
    }

    static func oneOf(_ middlewares: [MiddlewareProtocol], _ selector: OneOfMiddleware.Selector) -> OneOfMiddleware {
        return .init(middlewares: middlewares, selector: selector)
    }

    static func chainOf(_ middlewares: [MiddlewareProtocol]) -> ChainOfMiddleware {
        return .init(middlewares: middlewares)
    }

    static func cascadeOf(_ middlewares: [MiddlewareProtocol]) -> CascadeOfMiddleware {
        return .init(middlewares: middlewares)
    }

    static func onceOf(_ middleware: MiddlewareProtocol) -> OnceOfMiddleware {
        return .init(middleware: middleware)
    }

    static func repeatOf(_ middleware: MiddlewareProtocol, times: Int) -> RepeatOfMiddleware {
        return .init(middleware: middleware, times: times)
    }

    static func respondWith(_ response: HttpResponse) -> FixedResponseMiddleware {
        return .init(response: response)
    }

    static func expectation(_ expectation: XCTestExpectation) -> RequestExpectationMiddleware {
        return .init(expectation: expectation)
    }

    static func flatMap(_ transform: @escaping FlatMapMiddleware.Transform) -> FlatMapMiddleware {
        return .init(transform: transform)
    }

    static func callback(_ callback: @escaping (HttpRequest) -> Void) -> CallbackMiddleware {
        return .init(callback: callback)
    }

    private init() {
    }
}

final class MiddlewareContext {
    var values: [AnyHashable: Any]

    init() {
        self.values = [:]
    }

    init(_ object: MiddlewareContext) {
        self.values = object.values
    }
}

protocol MiddlewareProtocol {
    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext)
}

final class NoopMiddleware: MiddlewareProtocol {
    static let instance: NoopMiddleware = NoopMiddleware()

    func handler(request _: HttpRequest, response _: inout HttpResponse?, context _: MiddlewareContext) {
    }
}

final class RequestTimeMiddleware: MiddlewareProtocol {
    var requestTime: () -> TimeInterval

    init(requestTime: @escaping () -> TimeInterval) {
        self.requestTime = requestTime
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let requestTime = self.requestTime()
        if requestTime > 0 {
            Thread.sleep(forTimeInterval: requestTime)
        }
    }
}

final class OneOfMiddleware: MiddlewareProtocol {
    final class Selector {
        var selectedIndex: Int

        init() {
            self.selectedIndex = -1
        }
    }

    var middlewares: [MiddlewareProtocol]
    var selector: Selector

    init(middlewares: [MiddlewareProtocol], selector: Selector) {
        self.middlewares = middlewares
        self.selector = selector
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let middlewares = self.middlewares
        let selectedIndex = self.selector.selectedIndex
        guard middlewares.indices.contains(selectedIndex) else { return }

        let middleware = middlewares[selectedIndex]
        middleware.handler(request: request, response: &response, context: context)
    }
}

final class ChainOfMiddleware: MiddlewareProtocol {
    var middlewares: [MiddlewareProtocol]

    init(middlewares: [MiddlewareProtocol]) {
        self.middlewares = middlewares
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        for middleware in self.middlewares {
            middleware.handler(request: request, response: &response, context: context)
            if response != nil {
                break
            }
        }
    }
}

final class CascadeOfMiddleware: MiddlewareProtocol {
    var middlewares: [MiddlewareProtocol]
    var selectedIndex: Int

    init(middlewares: [MiddlewareProtocol]) {
        self.middlewares = middlewares
        self.selectedIndex = 0
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let middlewares = self.middlewares
        let selectedIndex = self.selectedIndex
        guard middlewares.indices.contains(selectedIndex) else { return }

        let middleware = middlewares[selectedIndex]
        middleware.handler(request: request, response: &response, context: context)

        self.selectedIndex = selectedIndex + 1
    }
}

final class OnceOfMiddleware: MiddlewareProtocol {
    var middleware: MiddlewareProtocol
    var isFinished: Bool

    init(middleware: MiddlewareProtocol) {
        self.middleware = middleware
        self.isFinished = false
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let middleware = self.middleware
        let isFinished = self.isFinished
        guard isFinished == false else { return }

        middleware.handler(request: request, response: &response, context: context)

        self.isFinished = true
    }
}

final class RepeatOfMiddleware: MiddlewareProtocol {
    var middleware: MiddlewareProtocol
    var times: Int
    var counter: Int

    init(middleware: MiddlewareProtocol, times: Int) {
        self.middleware = middleware
        self.times = times
        self.counter = 0
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let middleware = self.middleware
        let times = self.times
        let counter = self.counter
        guard counter < times else { return }

        middleware.handler(request: request, response: &response, context: context)

        self.counter = counter + 1
    }
}

final class FixedResponseMiddleware: MiddlewareProtocol {
    var response: HttpResponse

    init(response: HttpResponse) {
        self.response = response
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        response = self.response
    }
}

final class RequestExpectationMiddleware: MiddlewareProtocol {
    var expectation: XCTestExpectation

    init(expectation: XCTestExpectation) {
        self.expectation = expectation
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        self.expectation.fulfill()
    }
}

final class FlatMapMiddleware: MiddlewareProtocol {
    typealias Transform = (HttpRequest, HttpResponse?, MiddlewareContext) -> MiddlewareProtocol

    var transform: Transform

    init(transform: @escaping Transform) {
        self.transform = transform
    }

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        let middleware = self.transform(request, response, context)
        middleware.handler(request: request, response: &response, context: context)
    }
}

struct CallbackMiddleware: MiddlewareProtocol {
    let callback: (HttpRequest) -> Void

    func handler(request: HttpRequest, response: inout HttpResponse?, context: MiddlewareContext) {
        self.callback(request)
    }
}
