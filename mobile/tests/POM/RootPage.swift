import Foundation
import MarketWelcomeOnboardingFeature

final class RootPage: PageObject {

    var onboarding: WelcomeOnboardingPage {
        let elem = element
            .otherElements
            .matching(identifier: WelcomeOnboardingAccessibility.root)
            .firstMatch
        return WelcomeOnboardingPage(element: elem)
    }

    var tabBar: TabBarPage {
        let elem = element
            .otherElements
            .matching(identifier: TabBarAccessibility.tabBar)
            .firstMatch
        return TabBarPage(element: elem)
    }

    var hint: HintPage {
        let elem = element
            .otherElements
            .matching(identifier: HintAccessibility.root)
            .firstMatch
        return HintPage(element: elem)
    }
}
