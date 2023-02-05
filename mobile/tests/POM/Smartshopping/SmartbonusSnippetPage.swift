import XCTest

/// Ячейка купона в коллекции
final class SmartbonusSnippetPage: PageObject {

    // MARK: - Properties

    static let rootIdentifier = SmartbonusSnippetAccessibility.root

    var topPart: XCUIElement {
        element.otherElements.matching(identifier: SmartbonusSnippetAccessibility.topPart).firstMatch.otherElements
            .firstMatch
    }

    private var bottomPart: XCUIElement {
        element.otherElements.matching(identifier: SmartbonusSnippetAccessibility.bottomPart).firstMatch.otherElements
            .firstMatch
    }

    /// Картинка купона
    var bonusImage: XCUIElement {
        topPart.images.matching(identifier: SmartbonusSnippetAccessibility.bonusImage).firstMatch
    }

    /// Название купона
    var bonusTitle: XCUIElement {
        bottomPart.staticTexts.matching(identifier: SmartbonusSnippetAccessibility.bonusTitle).firstMatch
    }

    /// Подзаголовок купона
    var bonusSubtitle: XCUIElement {
        bottomPart.staticTexts.matching(identifier: SmartbonusSnippetAccessibility.bonusSubtitle).firstMatch
    }

    func tap() {
        element.tap()
    }

    func verifyContent(with title: String, subtitle: String) {
        XCTAssertTrue(bonusImage.exists)
        XCTAssertTrue(bonusTitle.isVisible)
        XCTAssertTrue(bonusSubtitle.isVisible)
        XCTAssertEqual(bonusTitle.label, title)
        XCTAssertEqual(bonusSubtitle.label, subtitle)
    }

}
