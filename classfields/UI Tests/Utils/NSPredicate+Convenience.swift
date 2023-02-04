//
//  Created by Alexey Aleshkov on 03/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

extension NSPredicate {
    static func yreClosure<Value>(
        _ closure: @escaping (_ object: Value?, _ arguments: [String: Any]?) -> Bool
    ) -> NSPredicate {
        let result = NSPredicate(block: { object, arguments -> Bool in
            closure(object as? Value, arguments)
        })
        return result
    }

    static func yreKeyPathValue<Root, Value>(
        _ keyPath: KeyPath<Root, Value>,
        _ type: NSComparisonPredicate.Operator,
        _ value: Value,
        modifier: NSComparisonPredicate.Modifier = .direct,
        options: NSComparisonPredicate.Options = []
    ) -> NSPredicate {
        let lhe = NSExpression(forKeyPath: keyPath)
        let rhe = NSExpression(forConstantValue: value)
        let predicate = NSComparisonPredicate(
            leftExpression: lhe,
            rightExpression: rhe,
            modifier: modifier,
            type: type,
            options: options
        )
        return predicate
    }
}
