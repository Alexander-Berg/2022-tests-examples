import XCTest

final class SmartbonusTokenCardSnippetPage: PageObject {

    // MARK: - Properties

    static let rootIdentifier = SmartbonusTokenAccessibility.root

    /// Название карточки
    var title: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusTokenAccessibility.title).firstMatch
    }

    var detailsButton: ActionButton {
        let buttonElem = element.buttons.matching(identifier: SmartbonusTokenAccessibility.actionButton).firstMatch
        return ActionButton(element: buttonElem)
    }
}

// MARK: - Nested types

extension SmartbonusTokenCardSnippetPage {

    class ActionButton: PageObject, CatalogEntryPoint {}

}
