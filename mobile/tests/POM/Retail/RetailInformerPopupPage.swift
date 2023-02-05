import UIUtils
import XCTest

final class RetailInformerPopupPage: PageObject, PopupPage {

    // MARK: - Properties

    static var rootIdentifier: String = RetailAccessibility.informerPopUp

    var informerShopTitle: XCUIElement {
        element
            .staticTexts
            .matching(identifier: RetailAccessibility.informerShopTitle)
            .firstMatch
    }

}
