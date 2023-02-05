import XCTest

final class SmartbonusDetailsPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = SmartbonusDetailsAccessibility.root

    /// Картинка купона
    var bonusImage: XCUIElement {
        element.images.matching(identifier: SmartbonusSnippetAccessibility.bonusImage).firstMatch
    }

    /// Название купона
    var bonusTitle: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusDetailsAccessibility.bonusTitle).firstMatch
    }

    /// Описание купона
    var bonusDescription: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusDetailsAccessibility.bonusDescription).firstMatch
    }

    /// Верхняя кнопка
    var topButton: XCUIElement {
        element.buttons.matching(identifier: SmartbonusDetailsAccessibility.topButton).firstMatch
    }

    /// Нижняя кнопка
    var bottomButton: XCUIElement {
        element.buttons.matching(identifier: SmartbonusDetailsAccessibility.bottomButton).firstMatch
    }

    var bonusConditionTitle: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusDetailsAccessibility.bonusConditionTitle).firstMatch
    }

    var lockImage: XCUIElement {
        element.images.matching(identifier: SmartbonusDetailsAccessibility.lockImage).firstMatch
    }

    var expirationDate: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusDetailsAccessibility.expirationDate).firstMatch
    }

    var deliveryLabel: XCUIElement {
        element.staticTexts.matching(identifier: SmartbonusDetailsAccessibility.deliveryLabel).firstMatch
    }

    var closeButton: XCUIElement {
        element.buttons.matching(identifier: SmartbonusDetailsAccessibility.closeButton).firstMatch
    }
}
