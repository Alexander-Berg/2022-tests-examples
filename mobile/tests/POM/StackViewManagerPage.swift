import Foundation
import XCTest

protocol StackViewManagerPage: PageObject {
    associatedtype AccessibilityIdentifierProvider: StackViewItemsAccessibility

    func item(at index: Int) -> XCUIElement
}

extension StackViewManagerPage {
    func item(at index: Int) -> XCUIElement {
        let identifier = AccessibilityIdentifierProvider.accessibilityIdentifier(forItemAt: index)
        return element.otherElements.matching(identifier: identifier).firstMatch
    }
}
