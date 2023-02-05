//
//  ReadonlyVariableTests.swift
//  YandexMapsRx-Unit-Tests
//
//  Created by Dmitry Trimonov on 29/06/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import RxCocoa
import RxSwift
import RxTest
import XCTest

@testable import YandexMapsRx

class ReadonlyVariableTests: XCTestCase {

    var scheduler: TestScheduler!
    var bag: DisposeBag!

    override func setUp() {
        super.setUp()
        scheduler = TestScheduler(initialClock: 0)
        bag = DisposeBag()
    }

    func testReadonlyVariableNotDisposingWhileSubscribed() {
        for testCase in TestCase.allCases {
            setUp()

            let input: [Recorded<Event<Int>>] = [.next(2, 2), .next(4, 4)]
            var (source, rv1) = testCase
                .makeSources(with: input, defaultValue: 0, scheduler: scheduler, bag: bag)

            let observer = scheduler.createObserver(Int.self)

            _ = source // Just for warning fix
            var expectedEvents: [Recorded<Event<Int>>]

            if testCase.isLateSubscription {
                expectedEvents = []

                scheduler.advanceTo(3)

                rv1!.subscribe(observer).disposed(by: bag)
                rv1 = nil

                expectedEvents.append(.next(3, 2))
                XCTAssertEqual(observer.events, expectedEvents)
            } else {
                rv1!.subscribe(observer).disposed(by: bag)

                scheduler.advanceTo(1)
                expectedEvents = [.next(0, 0)]
                XCTAssertEqual(observer.events, expectedEvents)

                rv1 = nil

                scheduler.advanceTo(3)
                expectedEvents.append(.next(2, 2))
                XCTAssertEqual(observer.events, expectedEvents)
            }

            source = nil

            scheduler.advanceTo(5)
            expectedEvents.append(.next(4, 4))
            XCTAssertEqual(observer.events, expectedEvents)

            tearDown()
        }
    }

    func testChainedReadonlyVariablesNotDisposingWhileSubscribed() {
        for testCase in TestCase.allCases {
            setUp()

            let input: [Recorded<Event<Int>>] = [.next(2, 2), .next(4, 4), .next(6, 6)]
            var (source, rv1) = testCase
                .makeSources(with: input, defaultValue: 0, scheduler: scheduler, bag: bag)

            var rv2: ReadonlyVariable<Double>? = rv1?.mapValues { Double($0) }

            let observer = scheduler.createObserver(Double.self)

            _ = source // Just for warning fix
            var expectedEvents: [Recorded<Event<Double>>]

            if testCase.isLateSubscription {
                expectedEvents = []

                scheduler.advanceTo(3)

                rv2!.subscribe(observer).disposed(by: bag)
                source = nil

                expectedEvents.append(.next(3, 2.0))
                XCTAssertEqual(observer.events, expectedEvents)
            } else {
                rv2!.subscribe(observer).disposed(by: bag)

                scheduler.advanceTo(1)
                expectedEvents = [.next(0, 0.0)]
                XCTAssertEqual(observer.events, expectedEvents)

                source = nil

                scheduler.advanceTo(3)
                expectedEvents.append(.next(2, 2.0))
                XCTAssertEqual(observer.events, expectedEvents)
            }

            rv1 = nil

            scheduler.advanceTo(5)
            expectedEvents.append(.next(4, 4.0))
            XCTAssertEqual(observer.events, expectedEvents)

            rv2 = nil

            scheduler.advanceTo(7)
            expectedEvents.append(.next(6, 6.0))
            XCTAssertEqual(observer.events, expectedEvents)

            tearDown()
        }
    }

    func testReadonlyVariableDisposingWhenSubscriptionIsDisposed() {
        for sourceType in TestCase.SourceType.allCases {
            setUp()

            let testCase = TestCase(sourceType: sourceType, readonlyType: .subscription, isLateSubscription: false)

            let input: [Recorded<Event<Int>>] = [.next(2, 2), .next(4, 4)]
            var (_, rv1) = testCase
                .makeSources(with: input, defaultValue: 0, scheduler: scheduler, bag: bag)

            let observer = scheduler.createObserver(Int.self)
            rv1!.subscribe(observer).disposed(by: bag)

            scheduler.advanceTo(5)
            XCTAssertEqual(observer.events, [.next(0, 0), .next(2, 2), .next(4, 4)])

            bag = nil

            guard let refCountDisposable = rv1?.subscriptionSource?.refCountDisposer else {
                XCTAssert(false)
                return
            }

            XCTAssertFalse(refCountDisposable.isDisposed)
            rv1 = nil
            XCTAssertTrue(refCountDisposable.isDisposed)

            tearDown()
        }
    }

}

fileprivate extension ReadonlyVariableTests {

    struct TestCase: CaseIterable {

        enum ReadonlyType: CaseIterable {
            case variable
            case relay
            case subscription

            func makeSources<E>(
                with sourceStream: Observable<E>,
                defaultValue: E,
                bag: DisposeBag) -> (source: AnyObject?, readonly: ReadonlyVariable<E>?)
            {
                switch self {
                case .variable:
                    let source = BehaviorRelay(value: defaultValue)
                    sourceStream.bind(to: source).disposed(by: bag)
                    return (source, source.asReadonly())
                case .relay:
                    let source = BehaviorRelay(value: defaultValue)
                    sourceStream.bind(to: source).disposed(by: bag)
                    return (source, source.asReadonly())
                case .subscription:
                    let readonly = ReadonlyVariable(value: defaultValue, changes: sourceStream)
                    return (sourceStream, readonly)
                }
            }
        }

        enum SourceType: CaseIterable {
            case cold
            case hot

            func makeSource<E>(with input: [Recorded<Event<E>>], scheduler: TestScheduler) -> Observable<E> {
                switch self {
                case .cold: return scheduler.createColdObservable(input).asObservable()
                case .hot: return scheduler.createHotObservable(input).asObservable()
                }
            }
        }

        let sourceType: SourceType
        let readonlyType: ReadonlyType
        let isLateSubscription: Bool

        static let allCases: [TestCase] = {
            ReadonlyType.allCases.flatMap { readonlyType in
                SourceType.allCases.flatMap { sourceType in
                    [false, true].map {
                        TestCase(sourceType: sourceType, readonlyType: readonlyType, isLateSubscription: $0)
                    }
                }
            }
        }()

        func makeSources<E>(
            with input: [Recorded<Event<E>>],
            defaultValue: E,
            scheduler: TestScheduler,
            bag: DisposeBag) -> (source: AnyObject?, readonly: ReadonlyVariable<E>?)
        {
            return readonlyType.makeSources(
                with: sourceType.makeSource(with: input, scheduler: scheduler),
                defaultValue: defaultValue,
                bag: bag
            )
        }
    }

}

fileprivate extension ReadonlyVariable {

    var subscriptionSource: ReadonlyVariable.SubscriptionSource<Element>? {
        switch self.source {
        case .subscription(let source): return source
        default: return nil
        }
    }

}
