import XCTest

final class GiftPickerViewPage: PageObject {
    class InfoButtonPage: PageObject, WebViewEntryPoint {}
    class GiftImageViewPage: PageObject, SKUEntryPoint {}

    var infoButton: InfoButtonPage {
        let item = element.buttons
            .matching(identifier: GiftPickerViewAccessibility.infoButton)
            .firstMatch
        return InfoButtonPage(element: item)
    }

    var checkButton: XCUIElement {
        element.buttons
            .matching(identifier: GiftPickerViewAccessibility.checkButton)
            .firstMatch
    }

    var offerImageView: XCUIElement {
        element.images
            .matching(identifier: GiftPickerViewAccessibility.offerImageView)
            .firstMatch
    }

    var giftImageView: GiftImageViewPage {
        let item = element.images
            .matching(identifier: GiftPickerViewAccessibility.giftImageView)
            .firstMatch
        return GiftImageViewPage(element: item)
    }

    var priceLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: GiftPickerViewAccessibility.priceLabel)
            .firstMatch
    }

    var plusLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: GiftPickerViewAccessibility.plusLabel)
            .firstMatch
    }

    var equalsLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: GiftPickerViewAccessibility.equalsLabel)
            .firstMatch
    }

    var titleLabel: XCUIElement {
        element.staticTexts
            .matching(identifier: GiftPickerViewAccessibility.titleLabel)
            .firstMatch
    }
}
