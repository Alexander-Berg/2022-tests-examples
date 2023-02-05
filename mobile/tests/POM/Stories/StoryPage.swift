import XCTest

class StoryPage: PageObject {

    static var current: StoryPage {
        let element = XCUIApplication().otherElements[StoryAccessibility.root]
        return StoryPage(element: element)
    }

    var closeButton: XCUIElement {
        element
            .buttons
            .matching(identifier: StoryAccessibility.closeButton)
            .firstMatch
    }

    var actionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: StoryAccessibility.actionButton)
            .firstMatch
    }

    func tapLeft() {
        let coordinate = element.coordinate(withNormalizedOffset: CGVector(dx: 0.1, dy: 0.5))
        coordinate.tap()
    }

    func tapRight() {
        let coordinate = element.coordinate(withNormalizedOffset: CGVector(dx: 0.9, dy: 0.5))
        coordinate.tap()
    }
}
