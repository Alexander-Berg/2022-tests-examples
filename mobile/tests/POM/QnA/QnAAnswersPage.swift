import UIUtils
import XCTest

final class QnAAnswersPage: PageObject {

    static var current: QnAAnswersPage {
        let element = XCUIApplication().any.matching(identifier: QnAAnswersAccessibility.root).firstMatch
        return QnAAnswersPage(element: element)
    }

    /// Шапка
    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    var showCommentsButton: XCUIElement {
        cellUniqueElement(withIdentifier: QnAAnswersAccessibility.showCommentsButton)
            .buttons
            .firstMatch
    }

    var answerAuthorCell: XCUIElement {
        cellUniqueElement(withIdentifier: QnAAnswersAccessibility.answerAuthorCell)
    }

    var cells: XCUIElementQuery {
        element.descendants(matching: .cell)
    }
}

// MARK: - CollectionViewPage

extension QnAAnswersPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = QnAAnswersCollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}

// MARK: - Nested Types

extension QnAAnswersPage {
    class NavigationBar: NavigationBarPage {
        override var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: QnAAnswersAccessibility.barTitle)
                .firstMatch
        }
    }
}
