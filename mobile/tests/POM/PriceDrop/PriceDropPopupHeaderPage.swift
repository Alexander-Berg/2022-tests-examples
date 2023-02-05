import XCTest

class PriceDropPopupHeaderPage: PageObject {

    var titleLabel: XCUIElement {
        element
            .staticTexts
            .matching(identifier: PriceDropPopupAccessibility.Header.titleHeaderPopup)
            .firstMatch
    }

    var subtitleLabel: XCUIElement {
        element
            .any
            .matching(identifier: PriceDropPopupAccessibility.Header.subtitleHeaderPopup)
            .firstMatch
    }
}
