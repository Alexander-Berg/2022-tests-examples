//
//  XCUIElement+Convenience.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

// swiftlint:disable opening_brace

extension XCUIElement {
    enum Direction {
        case up
        case down
        case left
        case right
    }

    func swipe(direction: Direction) {
        let half: CGFloat = 0.5
        let velocity: CGFloat = 0.25
        let pressDuration: TimeInterval = 0.05

        let lessThanHalf = half - velocity
        let moreThanHalf = half + velocity

        let centre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: half))

        switch direction {
            case .up:
                let aboveCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: lessThanHalf))
                centre.press(forDuration: pressDuration, thenDragTo: aboveCentre)

            case .down:
                let belowCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: moreThanHalf))
                centre.press(forDuration: pressDuration, thenDragTo: belowCentre)

            case .left:
                let leftOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: lessThanHalf, dy: half))
                centre.press(forDuration: pressDuration, thenDragTo: leftOfCentre)

            case .right:
                let rightOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: moreThanHalf, dy: half))
                centre.press(forDuration: pressDuration, thenDragTo: rightOfCentre)
        }
    }

    func scroll(by vector: CGVector, normalizedOffset: CGVector = .init(dx: 0.5, dy: 0.5)) {
        let pressDuration: TimeInterval = 0.05

        let centre = self.coordinate(withNormalizedOffset: normalizedOffset)
        let dragPoint = centre.withOffset(vector)

        centre.press(forDuration: pressDuration, thenDragTo: dragPoint)
    }

    func drag(by vector: CGVector, normalizedOffset: CGVector = .init(dx: 0.5, dy: 0.5)) {
        let pressDuration: TimeInterval = 0.05

        let centre = self.coordinate(withNormalizedOffset: normalizedOffset)
        let dragPoint = centre.withOffset(vector)

        centre.press(forDuration: pressDuration,
                     thenDragTo: dragPoint,
                     withVelocity: .slow,
                     thenHoldForDuration: pressDuration)
    }

    func scroll(
        to element: XCUIElement,
        adjustInteractionFrame: (CGRect) -> CGRect = { $0 },
        velocity: CGFloat = 1.0,
        swipeLimits: UInt = 5,
        normalizedOffset: CGVector = .init(dx: 0.5, dy: 0.5)
    ) {
        guard element.exists else { return }

        let scrollableView = self

        let scrollFrame = scrollableView.frame
        let interactionFrame = adjustInteractionFrame(scrollFrame)

        func intersectionPercent(of ofRect: CGRect, in inRect: CGRect) -> CGFloat {
            let intersection = ofRect.intersection(inRect)
            if intersection.isNull {
                return 0
            }
            if intersection.size == ofRect.size {
                return 1
            }
            let intersectionArea = intersection.size.width * intersection.size.height
            let ofRectArea = ofRect.size.width * ofRect.size.height
            let result = intersectionArea / ofRectArea
            return result
        }

        let interactionCenter = CGPoint(x: interactionFrame.midX, y: interactionFrame.midY)

        var numberOfSwipes: UInt = 0
        // swiftlint:disable:next line_length
        while (element.isHittable == false || intersectionPercent(of: element.frame, in: interactionFrame) < 1.0) && numberOfSwipes < swipeLimits {
            let elementCenter = CGPoint(x: element.frame.midX, y: element.frame.midY)
            let velocity = element.isHittable ? 1.0 : velocity
            let offset = CGVector(
                dx: (interactionCenter.x - elementCenter.x) * velocity,
                dy: (interactionCenter.y - elementCenter.y) * velocity
            )

            element.isHittable
                ? scrollableView.drag(by: offset, normalizedOffset: normalizedOffset)
                : scrollableView.scroll(by: offset, normalizedOffset: normalizedOffset)

            numberOfSwipes += 1
        }
    }
}

extension XCUIElement {
    func scrollToElement(
        element: XCUIElement,
        direction: Direction,
        swipeLimits: UInt = 5
    ) {
        var numberOfSwipes: UInt = 0
        while element.isHittable == false && numberOfSwipes < swipeLimits {
            self.swipe(direction: direction)
            numberOfSwipes += 1
        }
    }
}

extension XCUIElement {
    // Default method waitForExistence can return 'false' for already existed elements
    func yreWaitForExistence(timeout: TimeInterval = Constants.timeout) -> Bool {
        return self.exists || self.waitForExistence(timeout: timeout)
    }

    @discardableResult
    func yreEnsureExists(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.exists, .equalTo, true, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotExists(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.exists, .equalTo, false, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureExistsWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.exists, .equalTo, true, timeout: seconds, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotExistsWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.exists, .equalTo, false, timeout: seconds, message: message, file: file, line: line)
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureHittable(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isHittable, .equalTo, true, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotHittable(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isHittable, .equalTo, false, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureHittableWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isHittable, .equalTo, true, timeout: seconds, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotHittableWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isHittable, .equalTo, false, timeout: seconds, message: message, file: file, line: line)
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureVisible(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(
            \.isHittable,
            .equalTo,
            true,
            message: message,
            file: file,
            line: line
        )
    }

    @discardableResult
    func yreEnsureNotVisible(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(
            { $0.exists == false || $0.isHittable == false },
            message: message,
            file: file,
            line: line
        )
    }

    @discardableResult
    func yreEnsureVisibleWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(
            \.isHittable,
            .equalTo,
            true,
            timeout: seconds,
            message: message,
            file: file,
            line: line
        )
    }

    @discardableResult
    func yreEnsureNotVisibleWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(
            { $0.exists == false || $0.isHittable == false },
            timeout: seconds,
            message: message,
            file: file,
            line: line
        )
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureEnabled(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isEnabled, .equalTo, true, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotEnabled(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isEnabled, .equalTo, false, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureEnabledWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isEnabled, .equalTo, true, timeout: seconds, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotEnabledWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isEnabled, .equalTo, false, timeout: seconds, message: message, file: file, line: line)
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureLabelEqual(
        to label: String,
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.label, .equalTo, label, timeout: seconds, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureLabelStarts(
        with label: String,
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.label, .beginsWith, label, timeout: seconds, message: message, file: file, line: line)
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureValueEqual(
        to value: Any?,
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.value, .equalTo, value, timeout: seconds, message: message, file: file, line: line)
    }
}

extension XCUIElement {
    @discardableResult
    func yreEnsureSelected(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isSelected, .equalTo, true, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotSelected(
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsure(\.isSelected, .equalTo, false, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureSelectedWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isSelected, .equalTo, true, timeout: seconds, message: message, file: file, line: line)
    }

    @discardableResult
    func yreEnsureNotSelectedWithTimeout(
        timeout seconds: TimeInterval = Constants.timeout,
        message: String = "",
        file: StaticString = #file,
        line: UInt = #line
    ) -> Self {
        return self.yreEnsureWithTimeout(\.isSelected, .equalTo, false, timeout: seconds, message: message, file: file, line: line)
    }
}
