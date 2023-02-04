final class PublicProfileSuccessPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case settingsButton = "settings_button"
        case publicProfileButton = "public_profile_button"
    }

    static let rootElementID = "PublicProfileSuccessPopup"
    static let rootElementName = "Попап \"Вы — профессиональный продавец\""
}
