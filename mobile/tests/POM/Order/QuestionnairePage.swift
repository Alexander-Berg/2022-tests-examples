import UIUtils
import XCTest

final class QuestionnairePage: PageObject {

    // MARK: - Public

    var title: XCUIElement {
        cellUniqueElement(withIdentifier: QuestionnaireAccessibility.title)
            .textViews
            .firstMatch
    }

    var subtitle: XCUIElement {
        cellUniqueElement(withIdentifier: QuestionnaireAccessibility.subtitle)
            .textViews
            .firstMatch
    }

    var mainActionButton: MainActionButton {
        let el = cellUniqueElement(withIdentifier: QuestionnaireAccessibility.mainActionButton)
        return MainActionButton(element: el)
    }

    var extraActionButton: ExtraActionButton {
        let el = cellUniqueElement(withIdentifier: QuestionnaireAccessibility.extraActionButton)
        return ExtraActionButton(element: el)
    }

    static var current: QuestionnairePage {
        let item = XCUIApplication().otherElements[QuestionnaireAccessibility.root]
        return QuestionnairePage(element: item)
    }

}

protocol QuestionnairePageEntryPoint: PageObject {
    func tap() -> QuestionnairePage
}

extension QuestionnairePageEntryPoint {
    func tap() -> QuestionnairePage {
        QuestionnairePage.current
    }
}

// MARK: - CollectionViewPage

extension QuestionnairePage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = QuestionnaireAccessibilityCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }

}

// MARK: - Nested types

extension QuestionnairePage {

    class MainActionButton: PageObject {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

    class ExtraActionButton: PageObject {

        var button: XCUIElement {
            element.buttons.firstMatch
        }

    }

}
