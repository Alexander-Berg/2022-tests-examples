//
//  Created by Alexey Aleshkov on 03/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension XCTWaiter {
    static func yreWait(
        for expectations: [XCTestExpectation],
        timeout seconds: TimeInterval
    ) -> Bool {
        let waiterResult = Self.wait(for: expectations, timeout: seconds)
        let result = waiterResult == .completed
        return result
    }
}

extension XCUIElement {
    func yreWait(
        _ closure: @escaping (_ object: XCUIElement) -> Bool,
        timeout seconds: TimeInterval
    ) -> Bool {
        let predicate = NSPredicate.yreClosure({ (object: XCUIElement?, _) -> Bool in
            guard let object = object else { return false }
            let result = closure(object)
            return result
        })
        let expectation = self.yreExpect(predicate)
        let result = XCTWaiter.yreWait(for: [expectation], timeout: seconds)
        return result
    }

    func yreWait<Value>(
        _ keyPath: KeyPath<XCUIElement, Value>,
        _ type: NSComparisonPredicate.Operator,
        _ value: Value,
        timeout seconds: TimeInterval
    ) -> Bool {
        let predicate = NSPredicate.yreKeyPathValue(keyPath, type, value)
        let expectation = self.yreExpect(predicate)
        let result = XCTWaiter.yreWait(for: [expectation], timeout: seconds)
        return result
    }
}
