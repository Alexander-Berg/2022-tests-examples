//
//  Created by Alexey Aleshkov on 03/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension XCUIElement {
    func yreExpect<Value>(
        _ keyPath: KeyPath<XCUIElement, Value>,
        _ type: NSComparisonPredicate.Operator,
        _ value: Value
    ) -> XCTestExpectation {
        let predicate = NSPredicate.yreKeyPathValue(keyPath, type, value)
        let result = self.yreExpect(predicate)
        return result
    }
}

extension XCUIElement {
    func yreExpect(_ predicate: NSPredicate) -> XCTestExpectation {
        return XCTNSPredicateExpectation(predicate: predicate, object: self)
    }
}
