import MarketWelcomeOnboardingFeature
import XCTest

class RegionConfirmationPopupPage: PageObject {

    // MARK: - Properties

    static var current: RegionConfirmationPopupPage {
        let element = XCUIApplication().otherElements[RegionConfirmationPopupAccessibility.root]
        return RegionConfirmationPopupPage(element: element)
    }

    var title: XCUIElement {
        element.staticTexts.matching(identifier: RegionConfirmationPopupAccessibility.title).firstMatch
    }

    var confirmButton: XCUIElement {
        element.buttons.matching(identifier: RegionConfirmationPopupAccessibility.confirmButton).firstMatch
    }

    var rejectButton: XCUIElement {
        element.buttons.matching(identifier: RegionConfirmationPopupAccessibility.rejectButton).firstMatch
    }

}
