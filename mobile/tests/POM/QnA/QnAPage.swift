import UIUtils
import XCTest

final class QnAPage: PageObject {

    static var current: QnAPage {
        let element = XCUIApplication().any.matching(identifier: QnAAccessibility.root).firstMatch
        return QnAPage(element: element)
    }

    /// Шапка
    var navigationBar: NavigationBar {
        NavigationBar(element: NavigationBarPage.current.element)
    }

    /// Показать n ответов
    var showAllAnswers: XCUIElement {
        let element = cellUniqueElement(withIdentifier: QnAAccessibility.showAllAnswers)
        return element.buttons.firstMatch
    }

    /// Текст, если нет ответов
    var emptyAnswers: XCUIElement {
        let element = cellUniqueElement(withIdentifier: QnAAccessibility.emptyAnswers)
        return element.textViews.firstMatch
    }

    /// Кнопка "Комментировать"
    var addCommentButton: XCUIElement {
        let element = cellUniqueElement(withIdentifier: QnAAccessibility.addCommentButton)
        return element.textViews.firstMatch
    }

    var emptyView: XCUIElement {
        let query = element.otherElements.matching(identifier: QnAAccessibility.emptyView)
        return query.firstMatch
    }

    var emptyViewAskQuestionButton: XCUIElement {
        emptyView.buttons.firstMatch
    }
}

// MARK: - CollectionViewPage

extension QnAPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = QnACollectionViewCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested Types

extension QnAPage {
    class NavigationBar: NavigationBarPage {
        override var title: XCUIElement {
            element
                .staticTexts
                .matching(identifier: QnAAccessibility.barTitle)
                .firstMatch
        }
    }
}
