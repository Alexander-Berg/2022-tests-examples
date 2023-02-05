import MarketUI
import XCTest

/// Ячейка срока рассрочки в попапе селектора срока рассрочки
final class InstallmentsSelectorPopupTileCellPage: PageObject {

    /// Срок рассрочки
    var term: XCUIElement {
        element
            .staticTexts
            .matching(identifier: TiledSelectorAccessibility.title)
            .firstMatch
    }

    /// Ежемесячный платёж
    var monthlyPayment: XCUIElement {
        element
            .staticTexts
            .matching(identifier: TiledSelectorAccessibility.subtitle)
            .firstMatch
    }
}
