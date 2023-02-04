final class PublicProfileOnboardingPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case confirmButton = "confirm_button"
        case laterButton = "later_button"
    }

    static let rootElementID = "PublicProfileOnboardingPopup"
    static let rootElementName = "Попап \"Станьте профессиональным продавцом\""
}
