//
//  BaseScreen.swift
//  UITests
//
//  Created by Victor Orlovsky on 25/03/2019.
//
import XCTest
import Snapshots

protocol ScreenButton: RawRepresentable where RawValue == String {}

protocol ScreenWithButtons: BaseScreen {
    associatedtype ButtonType: RawRepresentable
}

class BaseScreen {
    var app: XCUIApplication

    required init() {
        self.app = XCUIApplication.make()
    }

    required init(_ app: XCUIApplication) {
        self.app = app
    }

    func on<T: BaseScreen>(screen: T.Type) -> T {
        let nextScreen: T

        // avoid duplicate initialization
        if self is T {
            nextScreen = self as! T
        } else {
            nextScreen = screen.init(app)
        }

        return nextScreen
    }

    func snapshot() -> UIImage {
        return app.waitAndScreenshot().image
    }
}

extension BaseScreen {
    func findAll(_ type: XCUIElement.ElementType) -> XCUIElementQuery {
        return app.descendants(matching: type)
    }

    func find(by identifier: String) -> XCUIElementQuery {
        return app.descendants(matching: .any).matching(identifier: identifier)
    }

    func findStaticText(by identifier: String) -> XCUIElement {
        return app.staticTexts[identifier]
    }

    func findContainedText(by identifier: String) -> XCUIElementQuery {
        return app.staticTexts.containingText(identifier)
    }

    func findContainedTextView(by identifier: String) -> XCUIElementQuery {
        return app.textViews.containingText(identifier)
    }

    func button<T: RawRepresentable>(_ kind: T) -> XCUIElement where T.RawValue == String {
        return find(by: kind.rawValue).firstMatch
    }
}

extension ScreenWithButtons {
    func button(_ kind: ButtonType) -> XCUIElement where ButtonType.RawValue == String {
        return find(by: kind.rawValue).firstMatch
    }
}

protocol Scrollable {
    var scrollableElement: XCUIElement { get }
}

extension Scrollable {
    func scrollToElementWith(text: String) -> XCUIElement {
        let element = self.scrollableElement.cells.staticTexts[text].firstMatch
        if self.scrollableElement.scrollTo(element: element, swipeDirection: .up) {
            return element
        } else {
            XCTFail("No element with text: \(text) on scrollable page \(String(describing: self))")
            return element
        }
    }

    @discardableResult
    func scrollTo(element: XCUIElement, swipeDirection: XCUIElement.SwipeDirection = .up) -> XCUIElement {
        scrollableElement.scrollTo(element: element, swipeDirection: swipeDirection)
        return element
    }

    @discardableResult
    func scrollTo(
        element: XCUIElement,
        maxSwipes: Int = 20,
        windowInsets: UIEdgeInsets = .zero,
        swipeDirection: XCUIElement.SwipeDirection = .up,
        afterSwipeCallBack: (() -> Void)? = nil,
        useLongSwipes: Bool = false,
        longSwipeAdjustment: Double = 0.4
    ) -> Bool {
        return scrollableElement.scrollTo(
            element: element,
            swipeDirection: swipeDirection,
            maxSwipes: maxSwipes,
            windowInsets: windowInsets,
            afterSwipeCallBack: afterSwipeCallBack,
            useLongSwipes: useLongSwipes,
            longSwipeAdjustment: longSwipeAdjustment
        )
    }

    func scrollToCell(at index: Int, swipeDirection: XCUIElement.SwipeDirection = .up) -> XCUIElement {
        let element = self.scrollableElement.cells.element(boundBy: index).firstMatch
        if self.scrollableElement.scrollTo(element: element, swipeDirection: swipeDirection) {
            return element
        } else {
            XCTFail("No cell at index: \(index) on scrollable page \(String(describing: self))")
            return element
        }
    }

    func swipe(_ direction: XCUIElement.SwipeDirection) {
        scrollableElement.gentleSwipe(direction)
    }
}

protocol NavigationControllerContent {
    var app: XCUIApplication { get }
}

extension NavigationControllerContent {
    var backButton: XCUIElement {
         return app.descendants(matching: .any).matching(identifier: "NavBarView").buttons.element(boundBy: 0)
    }
}
