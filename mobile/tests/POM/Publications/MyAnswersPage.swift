import UIUtils
import XCTest

class MyAnswersPage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: MyAnswersAccessibility.root).firstMatch
        return .init(element: element)
    }

    var emptyStateTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.title)
            .firstMatch
    }

    var openConfirmMenuButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.modelInfoCell)
            .buttons
            .firstMatch
    }

    var modelInfoCell: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.modelInfoCell)
    }

    var likeButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.likeButton)
    }

    var likeCount: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.likeButton)
            .textViews
            .firstMatch
    }

    var dislikeButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.dislikeButton)
    }

    var dislikeCount: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.dislikeButton)
            .textViews
            .firstMatch
    }

    var showCommentsButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyAnswersAccessibility.showCommentsButton)
            .buttons
            .firstMatch
    }

    func tapShowCommentsButton() -> QnAAnswersPage {
        showCommentsButton.tap()
        let element = XCUIApplication().any.matching(identifier: QnAAnswersAccessibility.root).firstMatch
        return QnAAnswersPage(element: element)
    }
}

// MARK: - CollectionViewPage

extension MyAnswersPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MyAnswersCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
