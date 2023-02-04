//
//  Double+Extension.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 08.07.2020.
//

import Foundation

infix operator ==~
extension Double {
    static func ==~ (lhs: Double, rhs: Double) -> Bool {
        fabs(lhs - rhs) < Double.ulpOfOne
    }
}
