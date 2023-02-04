//
//  Predicate.swift
//  UI Tests
//
//  Created by Alexey Salangin on 16.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

struct Predicate<Target>: CustomStringConvertible {
    var matches: (Target) -> Bool
    private(set) var description: String

    init(matcher: @escaping (Target) -> Bool, description: String = "Predicate") {
        self.matches = matcher
        self.description = description
    }

    static func && (lhs: Predicate<Target>, rhs: Predicate<Target>) -> Predicate<Target> {
        let matcher: (Target) -> Bool = { lhs.matches($0) && rhs.matches($0) }
        let description = [lhs.description, rhs.description].joined(separator: ", ")
        return Predicate(matcher: matcher, description: description)
    }

    static func || (lhs: Predicate<Target>, rhs: Predicate<Target>) -> Predicate<Target> {
        let matcher: (Target) -> Bool = { lhs.matches($0) || rhs.matches($0) }
        let description = [lhs.description, rhs.description].joined(separator: ", ")
        return Predicate(matcher: matcher, description: description)
    }
}
