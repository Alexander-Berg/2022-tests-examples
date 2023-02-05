import MarketUI
import UIUtils
import XCTest

///  Ячейка в каруселе категорий
class CategoryCarouselPage: PageObject {
    /// Название товара
    var titleLabel: XCUIElement {
        element.staticTexts.matching(identifier: CategoryCarouselAccessibility.title).firstMatch
    }
}
