import MarketOnboardingFeature
import XCTest

final class PromoOnboardingPage: PageObject, PopupPage {

    static var rootIdentifier: String = PromoOnboardingAccessibility.root

    var cross: XCUIElement {
        XCUIApplication()
            .buttons
            .matching(identifier: PromoOnboardingAccessibility.cross)
            .firstMatch
    }

    var actionButotn: XCUIElement {
        XCUIApplication()
            .buttons
            .matching(identifier: PromoOnboardingAccessibility.button)
            .firstMatch
    }
}
