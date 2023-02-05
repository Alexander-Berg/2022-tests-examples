import XCTest

class ImageActionNotificationViewPage: PageObject {
    class GoToCartButton: PageObject, CartEntryPoint {}

    var mainImageView: XCUIElement {
        element.images
            .matching(identifier: ImageActionNotificationAccessibility.mainImageView)
            .firstMatch
    }

    var secondaryImageView: XCUIElement {
        element.images
            .matching(identifier: ImageActionNotificationAccessibility.secondaryImageView)
            .firstMatch
    }

    var actionButton: GoToCartButton {
        let item = element.buttons
            .matching(identifier: ImageActionNotificationAccessibility.actionButton)
            .firstMatch
        return GoToCartButton(element: item)
    }
}

extension PageObject {
    var imageActionNotificationViewPage: ImageActionNotificationViewPage {
        let item = XCUIApplication().otherElements[ImageActionNotificationAccessibility.view]
        return ImageActionNotificationViewPage(element: item)
    }
}
