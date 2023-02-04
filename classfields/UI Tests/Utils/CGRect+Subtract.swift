//
//  CGRect+Subtract.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 20.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import CoreGraphics

extension CGRect {
    public func yreSubtract(_ rect: CGRect, from edge: CGRectEdge) -> CGRect {
        // Find how much `self` overlaps `rect`
        let intersection = self.intersection(rect)
        if intersection.isNull {
            // If they don't intersect, just return `self`. No subtraction to be done.
            return self
        }

        // Figure out how much we chop off r1
        let chopAmount: CGFloat
        switch edge {
            case .minXEdge:
                chopAmount = intersection.maxX - self.minX
            case .maxXEdge:
                chopAmount = self.maxX - intersection.minX
            case .minYEdge:
                chopAmount = intersection.maxY - self.minY
            case .maxYEdge:
                chopAmount = self.maxY - intersection.minY
        }

        // Chop
        let division = self.divided(atDistance: chopAmount, from: edge)
        let result = division.remainder
        return result
    }
}
