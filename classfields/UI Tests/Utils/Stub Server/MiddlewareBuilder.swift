//
//  Created by Alexey Aleshkov on 02/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import class Swifter.HttpRequest
import enum Swifter.HttpResponse
import YRECoreUtils

// swiftlint:disable opening_brace

final class MiddlewareBuilder {
    typealias FactoryClosure = () -> MiddlewareProtocol

    var builders: [FactoryClosure]

    init(builders: [FactoryClosure] = []) {
        self.builders = builders
    }

    func build() -> MiddlewareProtocol {
        let result: MiddlewareProtocol

        let middlewares = self.builders.map({
            $0()
        })
        if middlewares.isEmpty {
            result = Middlewares.noop()
        }
        else if middlewares.count == 1 {
            result = middlewares[0]
        }
        else {
            result = Middlewares.chainOf(middlewares)
        }

        return result
    }
}

extension MiddlewareBuilder {
    // MARK: - Just

    func just(_ middleware: MiddlewareProtocol) -> Self {
        self.builders.append({ middleware })
        return self
    }

    static func just(_ middleware: MiddlewareProtocol) -> Self {
        return .init(builders: [{ middleware }])
    }

    // MARK: - RequestTime

    func requestTime(_ requestTime: @escaping @autoclosure () -> TimeInterval) -> Self {
        self.builders.append({ RequestTimeMiddleware(requestTime: requestTime) })
        return self
    }

    static func requestTime(_ requestTime: @escaping @autoclosure () -> TimeInterval) -> Self {
        return .init(builders: [{ RequestTimeMiddleware(requestTime: requestTime) }])
    }

    // MARK: - OneOf

    func oneOf(_ middlewares: [MiddlewareBuilder], _ selector: OneOfMiddleware.Selector) -> Self {
        self.builders.append({
            OneOfMiddleware(middlewares: middlewares.map({ $0.build() }), selector: selector)
        })
        return self
    }

    static func oneOf(_ middlewares: [MiddlewareBuilder], _ selector: OneOfMiddleware.Selector) -> Self {
        return .init(builders: [
            {
                OneOfMiddleware(middlewares: middlewares.map({ $0.build() }), selector: selector)
            }
        ])
    }

    // MARK: - ChainOf

    func chainOf(_ middlewares: [MiddlewareBuilder]) -> Self {
        self.builders.append({
            ChainOfMiddleware(middlewares: middlewares.map({ $0.build() }))
        })
        return self
    }

    static func chainOf(_ middlewares: [MiddlewareBuilder]) -> Self {
        return .init(builders: [
            {
                ChainOfMiddleware(middlewares: middlewares.map({ $0.build() }))
            }
        ])
    }

    // MARK: - CascadeOf

    func cascadeOf(_ middlewares: [MiddlewareBuilder]) -> Self {
        self.builders.append({
            CascadeOfMiddleware(middlewares: middlewares.map({ $0.build() }))
        })
        return self
    }

    static func cascadeOf(_ middlewares: [MiddlewareBuilder]) -> Self {
        return .init(builders: [
            {
                CascadeOfMiddleware(middlewares: middlewares.map({ $0.build() }))
            }
        ])
    }

    // MARK: - OnceOf

    func onceOf(_ middleware: MiddlewareBuilder) -> Self {
        self.builders.append({
            OnceOfMiddleware(middleware: middleware.build())
        })
        return self
    }

    static func onceOf(_ middleware: MiddlewareBuilder) -> Self {
        return .init(builders: [
            {
                OnceOfMiddleware(middleware: middleware.build())
            }
        ])
    }

    // MARK: - RepeatOf

    func repeatOf(_ middleware: MiddlewareBuilder, times: Int) -> Self {
        self.builders.append({
            RepeatOfMiddleware(middleware: middleware.build(), times: times)
        })
        return self
    }

    static func repeatOf(_ middleware: MiddlewareBuilder, times: Int) -> Self {
        return .init(builders: [
            {
                RepeatOfMiddleware(middleware: middleware.build(), times: times)
            }
        ])
    }

    // MARK: - RespondWith

    func respondWith(_ provider: HttpResponseProvider) -> Self {
        let response = provider.generate()
        self.builders.append({ FixedResponseMiddleware(response: response) })
        return self
    }

    static func respondWith(_ provider: HttpResponseProvider) -> Self {
        let response = provider.generate()
        return .init(builders: [{ FixedResponseMiddleware(response: response) }])
    }

    // MARK: - Expectation

    func expectation(_ expectation: XCTestExpectation) -> Self {
        self.builders.append({ RequestExpectationMiddleware(expectation: expectation) })
        return self
    }

    static func expectation(_ expectation: XCTestExpectation) -> Self {
        return .init(builders: [{ RequestExpectationMiddleware(expectation: expectation) }])
    }

    // MARK: - FlatMap

    typealias FlatMapTransform = (HttpRequest, HttpResponse?, MiddlewareContext) -> MiddlewareBuilder

    func flatMap(_ transform: @escaping FlatMapTransform) -> Self {
        self.builders.append(
            {
                FlatMapMiddleware(transform: { transform($0, $1, $2).build() })
            }
        )
        return self
    }

    static func flatMap(_ transform: @escaping FlatMapTransform) -> Self {
        return .init(builders: [
            {
                FlatMapMiddleware(transform: { transform($0, $1, $2).build() })
            }
        ])
    }

    // MARK: - Callback
    func callback(_ callback: @escaping (HttpRequest) -> Void) -> Self {
        self.builders.append({ CallbackMiddleware(callback: callback) })
        return self
    }

    static func callback(_ callback: @escaping (HttpRequest) -> Void) -> Self {
        return .init(builders: [{ CallbackMiddleware(callback: callback) }])
    }
}

extension MiddlewareBuilder {
    enum PredicateStrategy {
        /// The next callback must match the critera
        case exactMatch
        /// One of the n callbacks must match the critera
        case numberOfCallbacks(Int)

        /// One of callbacks during the given time (in seconds) must match the critera
        case timeWindow(Int)
        /// Time window with default value
        case defaultTimeWindow
    }

    static func predicate(
        _ predicate: Predicate<HttpRequest>,
        stubFilename: String,
        strategy: PredicateStrategy = .numberOfCallbacks(5),
        handler: @escaping () -> Void
    ) -> MiddlewareBuilder {
        switch strategy {
            case .exactMatch:
                return self.predicate(
                    predicate,
                    stubFilename: stubFilename,
                    strategy: .numberOfCallbacks(1),
                    handler: handler
                )
            case let .numberOfCallbacks(number):
                return self.callbacksPredicate(
                    predicate,
                    stubFilename: stubFilename,
                    numberOfCallbacksExpected: number,
                    handler: handler
                )

            case .defaultTimeWindow:
                return self.predicate(
                    predicate,
                    stubFilename: stubFilename,
                    strategy: .timeWindow(Int(Constants.timeout)),
                    handler: handler
                )
            case let .timeWindow(secondsToWait):
                return self.timeWindowPredicate(
                    predicate,
                    stubFilename: stubFilename,
                    secondsToWait: secondsToWait,
                    handler: handler
                )
        }
    }

    private static func callbacksPredicate(
        _ predicate: Predicate<HttpRequest>,
        stubFilename: String,
        numberOfCallbacksExpected: Int,
        handler: @escaping () -> Void
    ) -> MiddlewareBuilder {
        var numberOfCallbacksReceived = 0
        var isSuccessfullyVerified = false

        let builders: [MiddlewareBuilder.FactoryClosure] = [
            MiddlewareBuilder.callback { request in
                DispatchQueue.main.async {
                    guard !isSuccessfullyVerified else { return }
                    numberOfCallbacksReceived += 1

                    isSuccessfullyVerified = Self.validateCallbackOfRequest(request, with: predicate)
                    if isSuccessfullyVerified {
                        handler()
                    }
                    else if numberOfCallbacksReceived >= numberOfCallbacksExpected {
                        XCTFail("\(numberOfCallbacksReceived) callback также не удовлетворяет предикату \(predicate.description)")
                    }
                    else {
                        // Increase the delay to fix this issue:
                        // https://st.yandex-team.ru/VSAPPS-7503
                        let delay = Int(2 * Constants.timeout)

                        let callbacksCount = numberOfCallbacksReceived
                        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(delay)) {
                            let noNewCallbacks = (numberOfCallbacksReceived == callbacksCount)
                            if noNewCallbacks {
                                XCTFail("Callback не удовлетворил предикату и не получили следующий callback за \(delay) с. Predicate => \(predicate.description)")
                            }
                        }
                    }
                }
            },
            MiddlewareBuilder.respondWith(.ok(.contentsOfJSON(stubFilename)))
        ].map { $0.build }
        return MiddlewareBuilder(builders: builders)
    }

    private static func timeWindowPredicate(
        _ predicate: Predicate<HttpRequest>,
        stubFilename: String,
        secondsToWait: Int,
        handler: @escaping () -> Void
    ) -> MiddlewareBuilder {
        var isSuccessfullyVerified = false

        DispatchQueue.main.asyncAfter(deadline: .now() + .seconds(secondsToWait)) {
            if !isSuccessfullyVerified {
                XCTFail("Не получили подходящий callback за \(secondsToWait) с. Predicate => \(predicate.description)")
            }
        }
        let builders: [MiddlewareBuilder.FactoryClosure] = [
            MiddlewareBuilder.callback { request in
                DispatchQueue.main.async {
                    guard !isSuccessfullyVerified else { return }

                    isSuccessfullyVerified = Self.validateCallbackOfRequest(request, with: predicate)
                    if isSuccessfullyVerified {
                        handler()
                    }
                }
            },
            MiddlewareBuilder.respondWith(.ok(.contentsOfJSON(stubFilename)))
        ].map { $0.build }
        return MiddlewareBuilder(builders: builders)
    }

    private static func validateCallbackOfRequest(
        _ request: HttpRequest,
        with predicate: Predicate<HttpRequest>
    ) -> Bool {
        return XCTContext.runActivity(named: "Проверка параметров сетевого запроса") { activity in
            let attachment = XCTAttachment(string: request.debugDescription)
            attachment.name = "Параметры в запросе"
            activity.add(attachment)

            let attachmentPredicate = XCTAttachment(string: predicate.description)
            attachmentPredicate.name = "Предикат"
            activity.add(attachmentPredicate)

            return predicate.matches(request)
        }
    }
}
