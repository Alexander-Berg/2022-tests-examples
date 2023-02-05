import MarketUI
import XCTest

final class CheapestAsGiftViewPage: PageObject {
    var imageView: XCUIElement {
        element.images
            .matching(identifier: CheapestAsGiftAccessibility.imageView)
            .firstMatch
    }
}
