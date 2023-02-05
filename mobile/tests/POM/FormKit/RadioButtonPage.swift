import FormKit
import MarketUI
import XCTest

class RadioButtonPage: PageObject {

    var image: XCUIElement {
        element.images.firstMatch
    }

    var title: XCUIElement {
        element.staticTexts
            .matching(identifier: RadioButtonViewCellAccessibility.radioTitleLabel).element
    }

    var subtitle: XCUIElement {
        element.staticTexts
            .matching(identifier: RadioButtonViewCellAccessibility.radioSubtitleLabel).element
    }

    var price: XCUIElement {
        element.staticTexts
            .matching(identifier: RadioButtonViewCellAccessibility.priceLabel).element
    }
}
