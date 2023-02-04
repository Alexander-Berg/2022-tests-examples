//
//  Created by Alexey Aleshkov on 03/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension XCUIElement {
    @discardableResult
    func yreEnsure(
        _ closure: @escaping (_ object: XCUIElement) -> Bool,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        let predicate = NSPredicate.yreClosure({ (object: XCUIElement?, _) -> Bool in
            guard let object = object else { return false }
            let result = closure(object)
            return result
        })
        let result = predicate.evaluate(with: self)
        if result == false {
            Self.logViewHierarchy()
        }
        XCTAssert(result, message, file: file, line: line)
        return self
    }

    @discardableResult
    func yreEnsure<Value>(
        _ keyPath: KeyPath<XCUIElement, Value>,
        _ type: NSComparisonPredicate.Operator,
        _ value: Value,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        let predicate = NSPredicate.yreKeyPathValue(keyPath, type, value)
        let result = predicate.evaluate(with: self)
        if result == false {
            Self.logViewHierarchy()
        }
        XCTAssert(result, message, file: file, line: line)
        return self
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureWithTimeout(
        _ closure: @escaping (_ object: XCUIElement) -> Bool,
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        let predicate = NSPredicate.yreClosure({ (object: XCUIElement?, _) -> Bool in
            guard let object = object else { return false }
            let result = closure(object)
            return result
        })
        let expectation = self.yreExpect(predicate)
        let result = predicate.evaluate(with: self) || XCTWaiter.yreWait(for: [expectation], timeout: seconds)
        if result == false {
            Self.logViewHierarchy()
        }
        XCTAssert(result, message, file: file, line: line)
        return self
    }

    @discardableResult
    func yreEnsureWithTimeout<Value>(
        _ keyPath: KeyPath<XCUIElement, Value>,
        _ type: NSComparisonPredicate.Operator,
        _ value: Value,
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        let predicate = NSPredicate.yreKeyPathValue(keyPath, type, value)
        let expectation = self.yreExpect(predicate)
        let result = predicate.evaluate(with: self) || XCTWaiter.yreWait(for: [expectation], timeout: seconds)
        if result == false {
            Self.logViewHierarchy()
        }
        XCTAssert(result, message, file: file, line: line)
        return self
    }
}

extension XCUIElement {
    fileprivate static func logViewHierarchy() {
        print("\(XCUIApplication().debugDescription)")
    }
}
