typealias GarageLandingScreen_ = GarageLandingSteps

extension GarageLandingScreen_: UIRootedElementProvider {
    static let rootElementID = "garage_landing"
    static let rootElementName = "Лэндинг гаража"

    enum Element: String {
        case addToGarageHeaderButton = "add_to_garage_header_button"
        case promoItem = "garage_landing_promo"
    }
}
