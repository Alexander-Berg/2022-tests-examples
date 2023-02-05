import MarketCashback
import XCTest

final class GrowingCashbackInfoPage: PageObject {

    static var current: GrowingCashbackInfoPage {
        let elem = XCUIApplication()
            .otherElements
            .matching(identifier: GrowingCashbackAccessibility.Information.root)
            .firstMatch
        return GrowingCashbackInfoPage(element: elem)
    }

    var closeButton: XCUIElement {
        element
            .buttons
            .matching(identifier: GrowingCashbackAccessibility.Information.closeButton)
            .firstMatch
    }

    var title: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Information.title)
            .firstMatch
    }

    var subtitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: GrowingCashbackAccessibility.Information.subtitle)
            .firstMatch
    }

    var actionButton: XCUIElement {
        element
            .buttons
            .matching(identifier: GrowingCashbackAccessibility.Information.actionButton)
            .firstMatch
    }
}

// MARK: - GrowingCashbackInfoPageEntryPoint

protocol GrowingCashbackInfoPageEntryPoint: PageObject {

    func tap() -> GrowingCashbackInfoPage
}

extension GrowingCashbackInfoPageEntryPoint {

    func tap() -> GrowingCashbackInfoPage {
        element.tap()
        return GrowingCashbackInfoPage.current
    }
}
