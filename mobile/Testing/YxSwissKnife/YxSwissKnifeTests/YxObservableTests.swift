import Foundation
import XCTest

@testable import YxSwissKnife

// NOTE: .inplace policy is needed for testing purposes, the structure of test won't be working with others

class YxBaseObsModel {
    var val1: YxObservable<Int> = YxObservable<Int>(value: 0, callbackPolicy: .inplace)
    var val2: YxObservable<Int?> = YxObservable<Int?>(value: nil, callbackPolicy: .inplace)
}

class YxArrayObsModel {
    var arr: YxObservableArray<Int> = YxObservableArray(value: [], callbackPolicy: .inplace)
}

class YxDictionaryObsModel {
    var dict: YxObservableDictionary<Int, String> = YxObservableDictionary(value: [:], callbackPolicy: .inplace)
}

//swiftlint:disable type_body_length
class YxBaseObservableTests: XCTestCase, YxObserver {

    var observerKey: String {
        return "YxBaseObservableTests"
    }

    //swiftlint:disable cyclomatic_complexity
    //swiftlint:disable function_body_length
    func testBaseObservable() {
        var step: Int = 0
        var checks: Int = 0

        let val1checks: (_ from: Int, _ to: Int) -> Void = { from, to in
            switch step {
            case 0:
                XCTAssertEqual(from, 0)
                XCTAssertEqual(to, 0)
                checks += 1

            case 1:
                XCTAssertEqual(from, 0)
                XCTAssertEqual(to, 10)
                checks += 1

            case 2:
                XCTAssertEqual(from, 10)
                XCTAssertEqual(to, 5)
                checks += 1

            default: break
            }
        }

        let val2checks: (_ from: Int?, _ to: Int?) -> Void = { from, to in
            switch step {
            case 0:
                XCTAssert(from == nil)
                XCTAssert(to == nil)
                checks += 1

            case 1:
                XCTAssert(from == nil)
                if let to = to {
                    XCTAssertEqual(to, 10)
                } else {
                    XCTAssert(false)
                }
                checks += 1

            case 2:
                if let from = from {
                    XCTAssertEqual(from, 10)
                } else {
                    XCTAssert(false)
                }
                XCTAssert(to == nil)
                checks += 1

            default: break
            }
        }

        let model: YxBaseObsModel = YxBaseObsModel()
        model.val1.add(observer: self, notify: false) { change in
            switch change {
            case let .will(from, to): val1checks(from, to)
            case let .did(from, to): val1checks(from, to)
            }
        }

        model.val2.add(observer: self, notify: false) { change in
            switch change {
            case let .will(from, to): val2checks(from, to)
            case let .did(from, to): val2checks(from, to)
            }
        }

        // step 0: default values (supplied by constructor) and custom raise
        step = 0
        model.val1.notify()
        model.val2.notify()

        // step 1: set values and use willSet, didSet raises
        step = 1
        model.val1.value = 10
        model.val2.value = 10

        // step 2: using overloaded operator
        step = 2
        model.val1 &= 5
        model.val2 &= nil

        // NOTE: 12 (not 4) because callbacks called twice - for .will and for .did
        XCTAssertEqual(checks, 12)
    }

    //swiftline:disable cyclomatic_complexity
    func testArrayObservable() {
        var step: Int = 0
        var checks: Int = 0

        let model: YxArrayObsModel = YxArrayObsModel()
        model.arr.add(observer: self, notify: false) { change in
            switch (change, step) {
            case (.will(let from, let to), 0):
                XCTAssert(from == [])
                XCTAssert(to == [])
                checks += 1

            case (.did(let from, let to), 0):
                XCTAssert(from == [])
                XCTAssert(to == [])
                checks += 1

            case (.will(let from, let to), 1):
                XCTAssert(from == [])
                XCTAssert(to == [1, 2])
                checks += 1

            case (.did(let from, let to), 1):
                XCTAssert(from == [])
                XCTAssert(to == [1, 2])
                checks += 1

            case (.will(let from, let to), 2):
                XCTAssert(from == [1, 2])
                XCTAssert(to == [3, 4])
                checks += 1

            case (.did(let from, let to), 2):
                XCTAssert(from == [1, 2])
                XCTAssert(to == [3, 4])
                checks += 1

            case (.add(let vals), 3):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[2] == 5)
                checks += 1

            case (.add(let vals), 4):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[0] == 2)
                checks += 1

            case (.add(let vals), 5): // NOTE: called with two items
                XCTAssert(vals.count == 2)
                XCTAssert(vals[1] == 6)
                XCTAssert(vals[2] == 7)
                checks += 1

            case (.move(let val, let from, let to), 6):
                XCTAssert(val == 6)
                XCTAssert(from == 1)
                XCTAssert(to == 2)
                checks += 1

            case (.remove(let vals), 7):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[1] == 7)
                checks += 1

            case (.remove(let vals), 8):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[4] == 5)
                checks += 1

            case (.change(let from, let to, let at), 9):
                XCTAssert(from == 6)
                XCTAssert(to == 2)
                XCTAssert(at == 1)
                checks += 1

            case (.remove(let vals), 10):
                XCTAssert(vals.count == 4)
                XCTAssert(vals[0] == 2)
                XCTAssert(vals[1] == 2)
                XCTAssert(vals[2] == 3)
                XCTAssert(vals[3] == 4)
                checks += 1

            default: break
            }
        }

        // step 0: default values (supplied by constructor) and custom raise
        step = 0
        model.arr.notify() // checks += 2 (2), will, did

        // step 1: set values and use willSet, didSet raises
        step = 1
        model.arr.value = [1, 2] // checks += 2 (4), will, did

        // step 2: using overloaded operator
        step = 2
        model.arr &= [3, 4] // checks += 2 (6), will, did

        // step 3: append to the last
        step = 3
        model.arr.append(5) // checks += 1 (7), add

        // step 4: append to the first
        step = 4
        model.arr.insert(2, at: 0) // checks += 1 (8), add

        // step 5: append array to the middle
        step = 5
        model.arr.insert(contentsOf: [6, 7], at: 1) // checks += 1 (9), add, add [2, 6, 7, 3, 4, 5]

        // step 6: move item
        step = 6
        model.arr.moveItem(from: 1, to: 2) // checks += 1 (10), .move

        // step 7: remove second item (7)
        step = 7
        model.arr.remove(at: 1) // checks += 1 (11), .remove [2, 6, 3, 4, 5]

        // step 8: remove last (5)
        step = 8
        model.arr.removeLast() // checks += 1 (12), .remove [2, 6, 3, 4]

        // step 9: subscript change [2, 6 -> 2, 3, 4]
        step = 9
        model.arr[1] = 2 // checks += 1 (13), .change

        // step 10: remove all
        step = 10
        model.arr.removeAll() // checks += 1 (14), .remove []

        XCTAssertEqual(checks, 14)
    }

    func testDictionaryObservable() {
        var step: Int = 0
        var checks: Int = 0

        let model: YxDictionaryObsModel = YxDictionaryObsModel()
        model.dict.add(observer: self, notify: false) { change in
            switch (change, step) {
            case (.will(let from, let to), 0):
                XCTAssert(from.count == 0)
                XCTAssert(to.count == 0)
                checks += 1

            case (.did(let from, let to), 0):
                XCTAssert(from.count == 0)
                XCTAssert(to.count == 0)
                checks += 1

            case (.will(let from, let to), 1):
                XCTAssert(from.count == 0)
                XCTAssert(to.count == 2)
                XCTAssert(to[1] == "Hello")
                XCTAssert(to[2] == "World")
                checks += 1

            case (.did(let from, let to), 1):
                XCTAssert(from.count == 0)
                XCTAssert(to.count == 2)
                XCTAssert(to[1] == "Hello")
                XCTAssert(to[2] == "World")
                checks += 1

            case (.will(let from, let to), 2):
                XCTAssert(from.count == 2)
                XCTAssert(from[1] == "Hello")
                XCTAssert(from[2] == "World")
                XCTAssert(to.count == 2)
                XCTAssert(to[3] == "Poka")
                XCTAssert(to[4] == "Mir")
                checks += 1

            case (.did(let from, let to), 2):
                XCTAssert(from.count == 2)
                XCTAssert(from[1] == "Hello")
                XCTAssert(from[2] == "World")
                XCTAssert(to.count == 2)
                XCTAssert(to[3] == "Poka")
                XCTAssert(to[4] == "Mir")
                checks += 1
                checks += 1

            case (.add(let vals), 3):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[5] == "Lesenka")
                checks += 1

            case (.change(let from, let to, let at), 4):
                XCTAssert(from == "Poka")
                XCTAssert(to == "Pesenka")
                XCTAssert(at == 3)
                checks += 1

            case (.remove(let vals), 5):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[5] == "Lesenka")
                checks += 1

            case (.move(let val, let from, let to), 6):
                if let val = val {
                    XCTAssert(val == "Pesenka")
                } else {
                    XCTAssert(false)
                }
                XCTAssert(from == 3)
                XCTAssert(to == 5)
                checks += 1

            case (.add(let vals), 7):
                XCTAssert(vals.count == 1)
                XCTAssert(vals[10] == "X")
                checks += 1

            case (.change(let from, let to, let at), 8):
                XCTAssert(from == "Pesenka")
                XCTAssert(to == "Hurray")
                XCTAssert(at == 5)
                checks += 1

            case (.remove(let vals), 9): // NOTE: should be called with three items
                XCTAssert(vals.count == 3)
                XCTAssert(vals[4] == "Mir")
                XCTAssert(vals[5] == "Hurray")
                XCTAssert(vals[10] == "X")
                checks += 1

            default: break
            }
        }

        // step 0: default values (supplied by constructor) and custom raise
        step = 0
        model.dict.notify() // checks += 2 (2), will, did

        // step 1: set values and use willSet, didSet raises
        step = 1
        model.dict.value = [1: "Hello", 2: "World"] // checks += 2 (4), will, did

        // step 2: using overloaded operator ([3: Poka, 4: Mir])
        step = 2
        model.dict &= [3: "Poka", 4: "Mir"] // checks += 2 (6), will, did

        // step 3: updateValue (that is never existed) ([3: Poka, 4: Mir, 5: Lesenka])
        step = 3
        model.dict.updateValue("Lesenka", forKey: 5) // checks += 1 (7), add

        // step 4: updateValue (that is existed) ([3: Pesenka, 4: Mir, 5: Lesenka])
        step = 4
        model.dict.updateValue("Pesenka", forKey: 3) // checks += 1 (8), change

        // step 5: removeValue ([3: Pesenka, 4: Mir])
        step = 5
        model.dict.removeValue(forKey: 5) // checks += 1 (9), remove

        // step 6: move item ([4: Mir, 5: Pesenka])
        step = 6
        model.dict.moveItem(fromKey: 3, toKey: 5) // checks += 1 (11), .move

        // step 7: subscript change (that is never existed) ([4: Mir, 5: Pesenka, 10: X])
        step = 7
        model.dict[10] = "X" // checks += 1 (12), add

        // step 8: subscript change (that is existed) ([4: Mir, 5: Hurray, 10: X])
        step = 8
        model.dict[5] = "Hurray" // checks += 1 (13), .change

        // step 9: remove all ([:])
        step = 9
        model.dict.removeAll() // checks += 1 (14), .remove []

        XCTAssertEqual(checks, 14)
    }
}
