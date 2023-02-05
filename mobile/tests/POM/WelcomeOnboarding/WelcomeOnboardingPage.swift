import MarketWelcomeOnboardingFeature
import XCTest

final class WelcomeOnboardingPage: PageObject {

    // MARK: - Properties

    static var current: WelcomeOnboardingPage {
        let welcomeOnboarding = XCUIApplication().otherElements[WelcomeOnboardingAccessibility.root]
        return WelcomeOnboardingPage(element: welcomeOnboarding)
    }

    var geoCell: XCUIElement {
        cellUniqueElement(withIdentifier: WelcomeOnboardingAccessibility.geoIdentifier)
            .firstMatch
    }

    var geoActionButton: XCUIElement {
        geoCell
            .buttons
            .matching(identifier: WelcomeOnboardingAccessibility.actionButton)
            .firstMatch
    }

    var geoAdditionalActionButton: XCUIElement {
        geoCell
            .buttons
            .matching(identifier: WelcomeOnboardingAccessibility.additionalButton)
            .firstMatch
    }

    var pushCell: XCUIElement {
        cellUniqueElement(withIdentifier: WelcomeOnboardingAccessibility.pushIdentifier)
            .firstMatch
    }

    var pushActionButton: XCUIElement {
        pushCell
            .buttons
            .matching(identifier: WelcomeOnboardingAccessibility.actionButton)
            .firstMatch
    }

    var pushAdditionalActionButton: XCUIElement {
        pushCell
            .buttons
            .matching(identifier: WelcomeOnboardingAccessibility.additionalButton)
            .firstMatch
    }

    var crossButton: XCUIElement {
        XCUIApplication()
            .buttons
            .matching(identifier: WelcomeOnboardingAccessibility.crossButton)
            .firstMatch
    }
}

// MARK: - CollectionViewPage

extension WelcomeOnboardingPage: CollectionViewPage {

    typealias AccessibilityIdentifierProvider = WelcomeOnboardingCellAccessibility

    var collectionView: XCUIElement {
        element.collectionViews.firstMatch
    }
}
