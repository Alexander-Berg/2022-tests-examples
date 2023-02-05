import Foundation
import UIUtils
import XCTest

final class BarrierViewPage: PageObject {

    var image: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.image)
            .firstMatch
    }

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.title)
            .firstMatch
    }

    var subtitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.subtitle)
            .firstMatch
    }

    var actionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: BarrierViewAccessibility.actionButton)
            .firstMatch
    }

    var extraButton: XCUIElement {
        element
            .buttons
            .matching(identifier: BarrierViewAccessibility.extraButton)
            .firstMatch
    }
}

extension BarrierViewPage {

    static let current: BarrierViewPage = .init(element: XCUIApplication().otherElements[BarrierViewAccessibility.root])

    func toToFinishedMultiorderPage() -> FinishMultiorderPage {
        element.swipeDown()

        return FinishMultiorderPage(
            element: XCUIApplication().otherElements[FinishMultiorderAccessibilty.root]
        )
    }
}
