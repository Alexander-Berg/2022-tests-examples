//
//  XCUIElement.swift
//  UITests
//
//  Created by Victor Orlovsky on 26/03/2019.
//

import XCTest
import UIKit

extension XCUIApplication {
    static func make() -> XCUIApplication {
        #if SWIFT_PACKAGE
        return XCUIApplication(bundleIdentifier: "ru.AutoRu")
        #else
        return XCUIApplication()
        #endif
    }
}

extension XCUIElement {
    public static let timeout = 3.0

    @discardableResult
    public func shouldExist(timeout: TimeInterval = timeout, message: String = "") -> XCUIElement {
        XCTAssertTrue(self.exists || self.waitForExistence(timeout: timeout), message)
        return self
    }

    @discardableResult
    public func shouldNotExist(timeout: TimeInterval = timeout, message: String = "") -> XCUIElement {
        var exists: Bool
        var time: TimeInterval = 0
        repeat {
            exists = self.exists
            sleep(1)
            time += 1
        } while exists && time < timeout

        XCTAssertFalse(exists, message)
        return self
    }

    @discardableResult
    public func containsText(_ text: String, timeout: TimeInterval = timeout, message: String = "") -> XCUIElement {
        XCTAssertTrue(self.label.contains(text))
        return self
    }

    @discardableResult
    public func shouldHaveKeyboardFocus() -> XCUIElement {
        let hasKeyboardFocus = (self.value(forKey: "hasKeyboardFocus") as? Bool) ?? false
        XCTAssert(hasKeyboardFocus, "Element should have keyboard focus")
        return self
    }

    @discardableResult
    public func shouldBeVisible(timeout: TimeInterval = timeout, windowInsets: UIEdgeInsets = .zero) -> XCUIElement {
        _ = self.shouldExist(timeout: timeout)
        XCTAssertTrue(self.isFullyVisible(windowInsets: windowInsets))
        return self
    }

    public func isFullyVisible(timeout: TimeInterval = timeout, windowInsets: UIEdgeInsets = .zero) -> Bool {
        guard self.waitForExistence(timeout: timeout) && !self.frame.isEmpty else {
            return false
        }

        let windowElementFrame = XCUIApplication.make().windows.element(boundBy: 0).frame.inset(by: windowInsets)
        return windowElementFrame.contains(self.frame)
    }

    @discardableResult
    public func shouldBe(enabled: Bool) -> XCUIElement {
        XCTAssertEqual(self.isEnabled, enabled)
        return self
    }

    @discardableResult
    public func shouldBe(disabled: Bool) -> XCUIElement {
        XCTAssertEqual(!self.isEnabled, disabled)
        return self
    }

    // MARK: - Text field

    public func clearText() {
        guard let stringValue = self.value as? String else {
            return
        }

        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)
        self.typeText(deleteString)
    }
    
    func adjustWheel(index: Int, value: String, timeout: TimeInterval = 0.5, maxScrolls: Int = 5) {
        let pickerWheel = pickerWheels.element(boundBy: index)
        let row = pickerWheels[value]
        
        var scrolls = 0
        while !row.waitForExistence(timeout: timeout),
              scrolls < maxScrolls {
            scrolls += 1
            pickerWheel.adjust(toPickerWheelValue: value)
        }
    }
    
    // MARK: - Collection view

    public func cell(containing id: String) -> XCUIElement {
        return self.cells.containing(NSPredicate(format: "identifier LIKE %@", id)).element(boundBy: 0)
    }

    // MARK: - Scroll

    public enum SwipeDirection: String {
        case up = "Вверх"
        case down = "Вниз"
        case right = "Вправо"
        case left = "Влево"
    }

    @discardableResult
    public func scrollTo(
        element: XCUIElement,
        swipeDirection: SwipeDirection,
        maxSwipes: Int = 20,
        initialTimeout: TimeInterval = timeout,
        timeout: TimeInterval = 0,
        windowInsets: UIEdgeInsets = .zero,
        velocity: XCUIGestureVelocity = .default,
        afterSwipeCallBack: (() -> Void)? = nil,
        useLongSwipes: Bool = false,
        longSwipeAdjustment: Double = 0.4
    ) -> Bool {
        if element.isFullyVisible(timeout: initialTimeout, windowInsets: windowInsets) {
            return true
        }

        var lastHierarchy = self.debugDescription
        for _ in 1...maxSwipes {
            if element.exists { // if element is on screen but not fully visible
                gentleSwipe(swipeDirection)
            } else if useLongSwipes {
                longSwipe(swipeDirection, adjustment: longSwipeAdjustment)
            } else {
                swipe(direction: swipeDirection, velocity: velocity)
            }

            if element.isFullyVisible(timeout: timeout, windowInsets: windowInsets) {
                return true
            }

            // если иерархия вьюх не поменялась - значит мы доскроли доконца
            let currentHierarhy = self.debugDescription
            if currentHierarhy == lastHierarchy {
                return false
            }
            lastHierarchy = currentHierarhy
            afterSwipeCallBack?()
        }
        return false
    }

    public func swipe(_ direction: XCUIElement.SwipeDirection, while predicate: (Int) -> Bool) {
        var numSwipes: Int = 0
        while predicate(numSwipes) {
            gentleSwipe(direction)
            numSwipes += 1
        }
    }

    public func gentleSwipe(_ direction: SwipeDirection) {
        let half: CGFloat = 0.5
        let adjustment: CGFloat = 0.20
        let pressDuration: TimeInterval = 0.05

        let lessThanHalf = half - adjustment
        let moreThanHalf = half + adjustment

        let centre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: half))
        let aboveCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: lessThanHalf))
        let belowCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: moreThanHalf))
        let leftOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: lessThanHalf, dy: half))
        let rightOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: moreThanHalf, dy: half))

        switch direction {
        case .up:
            centre.press(forDuration: pressDuration, thenDragTo: aboveCentre)
            break
        case .down:
            centre.press(forDuration: pressDuration, thenDragTo: belowCentre)
            break
        case .left:
            centre.press(forDuration: pressDuration, thenDragTo: leftOfCentre)
            break
        case .right:
            centre.press(forDuration: pressDuration, thenDragTo: rightOfCentre)
            break
        }
    }
    
    public func longSwipe(_ direction: SwipeDirection, adjustment: Double = 0.4) {
        let half: CGFloat = 0.5
        let pressDuration = 0.1
        
        let lessThanHalf = half - adjustment
        let moreThanHalf = half + adjustment

        let aboveCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: lessThanHalf))
        let belowCentre = self.coordinate(withNormalizedOffset: CGVector(dx: half, dy: moreThanHalf))
        let leftOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: lessThanHalf, dy: half))
        let rightOfCentre = self.coordinate(withNormalizedOffset: CGVector(dx: moreThanHalf, dy: half))

        switch direction {
        case .up:
            belowCentre.press(forDuration: pressDuration, thenDragTo: aboveCentre)
            break
        case .down:
            aboveCentre.press(forDuration: pressDuration, thenDragTo: belowCentre)
            break
        case .left:
            rightOfCentre.press(forDuration: pressDuration, thenDragTo: leftOfCentre)
            break
        case .right:
            leftOfCentre.press(forDuration: pressDuration, thenDragTo: rightOfCentre)
            break
        }
    }
    
    public func dragSwipe(_ direction: SwipeDirection, adjustment: CGFloat, pressDuration: TimeInterval) {
        
    }

    public func swipe(direction: SwipeDirection, velocity: XCUIGestureVelocity = .default) {
        switch direction {
        case .up:
            swipeUp(velocity: velocity)
        case .down:
            swipeDown(velocity: velocity)
        case .right:
            swipeRight(velocity: velocity)
        case .left:
            swipeLeft(velocity: velocity)
        }
    }

    public func clearAndEnterText(_ text: String) {
        guard let stringValue = self.value as? String else {
            XCTFail("Пытаемся стереть с элемента без значения")
            return
        }

        self.tap()

        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: stringValue.count)

        self.typeText(deleteString)
        self.typeText(text)
    }

    public func forceTap() {
        if isHittable {
            tap()
        } else {
            let coordinate: XCUICoordinate = self.coordinate(withNormalizedOffset: .zero)
            coordinate.tap()
        }
    }
}

extension XCUIElementQuery {
    public func containingText(_ text: String) -> XCUIElementQuery {
        let predicate = NSPredicate(format: "label CONTAINS '\(text)'")
        return self.containing(predicate)
    }

    public func withIdentifierPrefix(_ prefix: String) -> XCUIElementQuery {
        let predicate = NSPredicate(format: "identifier BEGINSWITH %@", prefix)
        return self.matching(predicate)
    }
}
