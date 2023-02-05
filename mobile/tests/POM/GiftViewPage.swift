import MarketUI
import XCTest

class GiftViewPage: PageObject {
    class GiftImageViewPage: PageObject, SKUEntryPoint {}

    var imageView: GiftImageViewPage {
        let item = element.images
            .matching(identifier: GiftViewAccessibility.imageView)
            .firstMatch
        return GiftImageViewPage(element: item)
    }

    var label: XCUIElement {
        element.staticTexts
            .matching(identifier: GiftViewAccessibility.label)
            .firstMatch
    }
}
