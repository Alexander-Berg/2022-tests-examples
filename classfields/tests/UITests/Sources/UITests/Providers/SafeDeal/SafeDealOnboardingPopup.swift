import XCTest

final class SafeDealOnboardingPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case carousel = "safe_deal_onboarding_carousel"
        case details = "safe_deal_onboarding_details_button"
        case understand = "safe_deal_onboarding_understand_button"
    }

    static let rootElementName = "Онбординг БС"
    static let rootElementID = "safe_deal_onboarding"
}
