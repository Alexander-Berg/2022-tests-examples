//
//  LoadingValueTests.swift
//  YandexMapsRx-Unit-Tests
//
//  Created by Alexander Ermichev on 9/3/21.
//

import RxCocoa
import RxSwift
import RxTest
import XCTest

@testable import YandexMapsRx

class LoadingValueTests: XCTestCase {

    var scheduler: TestScheduler!
    var bag: DisposeBag!

    override func setUp() {
        super.setUp()
        scheduler = TestScheduler(initialClock: 0)
        bag = DisposeBag()
    }

    override func tearDown() {
        super.tearDown()
        bag = nil
    }

    func testLoadingValueNotDisposingWhileSubscribed() {
        for testCase in TestCase.allCases {
            setUp()

            let input: [Recorded<Event<Int>>] = [.next(2, 2), .next(4, 4)]
            var lv1 = testCase
                .makeValue(with: input, defaultValueIfNeeded: 0, scheduler: scheduler)

            let observer = scheduler.createObserver(Int.self)
            var expectedEvents: [Recorded<Event<Int>>]

            if testCase.isLateSubscription {
                expectedEvents = []

                scheduler.advanceTo(3)

                lv1!.observable.subscribe(observer).disposed(by: bag)
                lv1 = nil

                expectedEvents.append(.next(3, 2))
                XCTAssertEqual(observer.events, expectedEvents)
            } else {
                lv1!.observable.subscribe(observer).disposed(by: bag)

                scheduler.advanceTo(1)
                expectedEvents = testCase.loadingValueType == .loaded ? [.next(0, 0)] : []
                XCTAssertEqual(observer.events, expectedEvents)

                lv1 = nil

                scheduler.advanceTo(3)
                expectedEvents.append(.next(2, 2))
                XCTAssertEqual(observer.events, expectedEvents)
            }

            scheduler.advanceTo(5)
            expectedEvents.append(.next(4, 4))
            XCTAssertEqual(observer.events, expectedEvents)

            tearDown()
        }
    }

    func testLoadingValueDisposingWhenSubscriptionIsDisposed() {
        for testCase in TestCase.allCases {
            guard !testCase.isLateSubscription else { return }

            setUp()

            let input: [Recorded<Event<Int>>] = [.next(2, 2), .next(4, 4)]
            var lv1 = testCase
                .makeValue(with: input, defaultValueIfNeeded: 0, scheduler: scheduler)

            let observer = scheduler.createObserver(Int.self)
            var expectedEvents: [Recorded<Event<Int>>]

            lv1!.observable.subscribe(observer).disposed(by: bag)
            expectedEvents = testCase.loadingValueType == .loaded ? [.next(0, 0)] : []

            scheduler.advanceTo(5)

            expectedEvents += [.next(2, 2), .next(4, 4)]
            XCTAssertEqual(observer.events, expectedEvents)

            bag = nil

            guard let refCountDisposable = lv1?.refCountDisposer else {
                XCTAssert(false)
                return
            }

            XCTAssertFalse(refCountDisposable.isDisposed)
            lv1 = nil
            XCTAssertTrue(refCountDisposable.isDisposed)

            tearDown()
        }
    }

}

fileprivate extension LoadingValueTests {

    struct TestCase: CaseIterable {

        enum LoadingValueType: CaseIterable {
            case loading
            case loaded

            func makeValue<E>(
                with sourceStream: Observable<E>,
                defaultValueIfNeeded defaultValue: E) -> LoadingValue<E>
            {
                switch self {
                case .loading:
                    return sourceStream.asLoadingValue()
                case .loaded:
                    return LoadingValue(
                        initialState: .loaded(value: defaultValue),
                        stream: sourceStream
                    )
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
        let loadingValueType: LoadingValueType
        let isLateSubscription: Bool

        static let allCases: [TestCase] = {
            LoadingValueType.allCases.flatMap { loadingValueType in
                SourceType.allCases.flatMap { sourceType in
                    [false, true].map {
                        TestCase(sourceType: sourceType, loadingValueType: loadingValueType, isLateSubscription: $0)
                    }
                }
            }
        }()

        func makeValue<E>(
            with input: [Recorded<Event<E>>],
            defaultValueIfNeeded defaultValue: E,
            scheduler: TestScheduler) -> LoadingValue<E>?
        {
            return loadingValueType.makeValue(
                with: sourceType.makeSource(with: input, scheduler: scheduler),
                defaultValueIfNeeded: defaultValue
            )
        }
    }

}
