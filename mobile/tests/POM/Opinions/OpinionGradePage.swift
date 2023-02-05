import UIUtils
import XCTest

class OpinionGradePage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: OpinionGradeAccessibility.root).firstMatch
        return .init(element: element)
    }

    func tapStars() {
        element.otherElements.matching(identifier: RateViewAccessibility.root).firstMatch.tap()
    }
}

// MARK: - CollectionViewPage

extension OpinionGradePage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = OpinionGradeViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
