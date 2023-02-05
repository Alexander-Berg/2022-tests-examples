//
//  ReadonlyVariableCombineLatestTests.swift
//  YandexMapsRx-Unit-Tests
//
//  Created by Alexander Ermichev on 22.06.2020.
//

import RxCocoa
import RxSwift
import RxTest
import XCTest
import YandexMapsRx

class ReadonlyVariableCombineLatestTests: XCTestCase {

    var scheduler: TestScheduler!
    var bag: DisposeBag!

    override func setUp() {
        super.setUp()
        scheduler = TestScheduler(initialClock: 0)
        bag = DisposeBag()
    }

    func testTwoSourcesCombineLatest() {
        let input1 = makeRelay(fromValue: 0, events: [.next(2, 2), .next(4, 4)])
        let input2 = makeReadonly(fromValue: 0, events: [.next(1, 1), .next(5, 5)])

        let output = ReadonlyVariable<Int>
            .combineLatestValues(input1, input2) { $0 * $1 }

        let observer = scheduler.createObserver(Int.self)
        output.subscribe(observer).disposed(by: bag)

        scheduler.advanceTo(6)
        XCTAssertEqual(observer.events, [
            .next(0, 0),
            .next(1, 0),
            .next(2, 2),
            .next(4, 4),
            .next(5, 20)
        ])
    }

    func testThreeSourcesCombineLatest() {
        let input1 = makeRelay(fromValue: 0, events: [.next(1, 1), .next(4, 4)])
        let input2 = makeRelay(fromValue: 0, events: [.next(2, 2), .next(5, 5)])
        let input3 = makeReadonly(fromValue: 0, events: [.next(3, 3), .next(6, 6)])

        let output = ReadonlyVariable<Int>
            .combineLatestValues(input1, input2, input3) { $0 * $1 * $2 }

        let observer = scheduler.createObserver(Int.self)
        output.subscribe(observer).disposed(by: bag)

        scheduler.advanceTo(7)
        XCTAssertEqual(observer.events, [
            .next(0, 0),
            .next(1, 0),
            .next(2, 0),
            .next(3, 6),
            .next(4, 24),
            .next(5, 60),
            .next(6, 120)
        ])
    }

    func testCollectionCombineLatest() {
        let input1 = makeReadonly(fromValue: 0, events: [.next(1, 1), .next(4, 4)])
        let input2 = makeReadonly(fromValue: 0, events: [.next(2, 2), .next(5, 5)])
        let input3 = makeReadonly(fromValue: 0, events: [.next(3, 3), .next(6, 6)])

        let output = ReadonlyVariable<Int>
            .combineLatestValues([input1, input2, input3]) { $0.reduce(1) { return $0 * $1 } }

        let observer = scheduler.createObserver(Int.self)
        output.subscribe(observer).disposed(by: bag)

        scheduler.advanceTo(7)
        XCTAssertEqual(observer.events, [
            .next(0, 0),
            .next(1, 0),
            .next(2, 0),
            .next(3, 6),
            .next(4, 24),
            .next(5, 60),
            .next(6, 120)
        ])
    }

}

fileprivate extension ReadonlyVariableCombineLatestTests {

    func makeRelay<E>(fromValue value: E, events: [Recorded<Event<E>>]) -> BehaviorRelay<E> {
        let result = BehaviorRelay(value: value)

        scheduler.createColdObservable(events)
            .asObservable()
            .bind(to: result)
            .disposed(by: bag)

        return result
    }

    func makeReadonly<E>(fromValue value: E, events: [Recorded<Event<E>>]) -> ReadonlyVariable<E> {
        return ReadonlyVariable(
            value: value,
            changes: scheduler.createColdObservable(events).asObservable()
        )
    }

}
