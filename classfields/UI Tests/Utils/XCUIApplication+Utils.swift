//
//  XCUIApplication+Utils.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 17.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import UIKit
import XCTest

extension XCUIApplication {
    func yre_ignoredEdges() -> UIEdgeInsets {
        switch self.windows.element(boundBy: 0).frame.size {
            // iPhone X
            case CGSize(width: 375, height: 812):
                let scale: CGFloat = 3
                return UIEdgeInsets(top: 44 * scale, left: 0, bottom: 34 * scale, right: 0)
            // iPhone 11
            case CGSize(width: 414, height: 896):
                let scale: CGFloat = 2
                return UIEdgeInsets(top: 48 * scale, left: 0, bottom: 34 * scale, right: 0)
            // iPhone SE
            case CGSize(width: 320, height: 568):
                let scale: CGFloat = 2
                return UIEdgeInsets(top: 20 * scale, left: 0, bottom: 0, right: 0)
            default:
                XCTFail("Unknown window size.")
                return .zero
        }
    }
}
