import UIUtils
import XCTest

class MyQuestionsPage: PageObject {

    static var current: Self {
        let element = XCUIApplication().any.matching(identifier: MyQuestionsAccessibility.root).firstMatch
        return .init(element: element)
    }

    var emptyStateTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: BarrierViewAccessibility.title)
            .firstMatch
    }

    var modelInfoCell: XCUIElement {
        cellUniqueElement(withIdentifier: MyQuestionsAccessibility.modelInfoCell)
    }

    var likeButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyQuestionsAccessibility.likeButton)
    }

    var likeCount: XCUIElement {
        cellUniqueElement(withIdentifier: MyQuestionsAccessibility.likeButton)
            .textViews
            .firstMatch
    }

    var showAnswersButton: XCUIElement {
        cellUniqueElement(withIdentifier: MyQuestionsAccessibility.showAnswersButton)
            .buttons
            .firstMatch
    }

    func tapShowAnswersButton() -> QnAAnswersPage {
        showAnswersButton.tap()
        let element = XCUIApplication().any.matching(identifier: QnAAnswersAccessibility.root).firstMatch
        return QnAAnswersPage(element: element)
    }
}

// MARK: - CollectionViewPage

extension MyQuestionsPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = MyQuestionsCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}
