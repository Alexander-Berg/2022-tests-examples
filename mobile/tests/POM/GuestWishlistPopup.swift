import XCTest

final class GuestWishlistPopup: PageObject, PopupPage {

    // MARK: - Properties

    static let rootIdentifier = GuestWishlistPopupAccessibility.root

    var cancelButton: XCUIElement {
        element
            .buttons.matching(identifier: GuestWishlistPopupAccessibility.cancelButton)
            .firstMatch
    }

    var loginButton: XCUIElement {
        element
            .buttons.matching(identifier: GuestWishlistPopupAccessibility.loginButton)
            .firstMatch
    }

    var titleLabel: XCUIElement {
        element
            .staticTexts.matching(identifier: GuestWishlistPopupAccessibility.titleLabel)
            .firstMatch
    }

    var descriptionLabel: XCUIElement {
        element
            .staticTexts.matching(identifier: GuestWishlistPopupAccessibility.descriptionLabel)
            .firstMatch
    }
}
