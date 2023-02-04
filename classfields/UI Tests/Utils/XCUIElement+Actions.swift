//
//  Created by Alexey Aleshkov on 03/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest

extension XCUIElement {
    // https://stackoverflow.com/a/58664462 in case if this method fails with bad_access
    @discardableResult
    func yreTypeText(_ text: String) -> Self {
        self.typeText(text)
        return self
    }

    @discardableResult
    func yreClearText() -> Self {
        guard let stringValue = self.value as? String, !stringValue.isEmpty else {
            return self
        }

        // On iOS 14 cursor is at the beginning of existing text by default - let's try to move it to the text's end
        let lowerRightCorner = self.coordinate(withNormalizedOffset: CGVector(dx: 0.99, dy: 0.99))
        lowerRightCorner.tap()

        let deleteString = [String](repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)
        self.typeText(deleteString.joined())

        return self
    }

    @discardableResult
    func yreTap() -> Self {
        self.tap()
        return self
    }

    @discardableResult
    func yreSwipeLeft() -> Self {
        self.swipeLeft()
        return self
    }

    @discardableResult
    func yrePullToRefresh() -> Self {
        let start = self.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0))
        let end = self.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        start.press(forDuration: 0, thenDragTo: end)
        return self
    }

    @discardableResult
    func yreForceTap() -> Self {
        if self.isHittable {
            self.tap()
        }
        else {
            let coordinate: XCUICoordinate = self.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
            coordinate.tap()
        }
        
        return self
    }
}

extension XCUIApplication {
    @discardableResult
    func yreTapStatusBar() -> Self {
        // @l-saveliy: On iOS 13 we can't get status bar as self.statusBars.firstMatch.tap()
        // Tap on rigth corner of status bar. It should work on both iphone 8 and X
        // We don't tap on left corner becase back button to previous app can be here
        // I can't find better solution. If you have any idea, fix it
        self.coordinate(withNormalizedOffset: CGVector(dx: 0.95, dy: 0)).tap()
        return self
    }
}
