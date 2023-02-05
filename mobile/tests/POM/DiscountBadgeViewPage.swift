import XCTest

class DiscountBadgeViewPage: PageObject {

    var root: XCUIElement {
        element
            .otherElements
            .matching(identifier: DiscountBadgeViewAccessibility.root)
            .firstMatch
    }

    // Процент скидки
    var discount: XCUIElement {
        element
            .staticTexts
            .matching(identifier: DiscountBadgeViewAccessibility.discount)
            .firstMatch
    }

    // Основной бейджик скидки
    var mainSaleBadge: XCUIElement {
        element
            .images
            .matching(identifier: DiscountBadgeViewAccessibility.mainSaleBadgeImage)
            .firstMatch
    }

    // Дополнительный бейджик скидки
    var additionalSaleBadge: XCUIElement {
        element
            .images
            .matching(identifier: DiscountBadgeViewAccessibility.additionalSaleBadgeImage)
            .firstMatch
    }
}
