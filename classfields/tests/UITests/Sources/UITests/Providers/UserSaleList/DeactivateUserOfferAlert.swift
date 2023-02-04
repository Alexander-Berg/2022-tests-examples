final class DeactivateUserOfferAlert: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case reactivateLater = "Продам позже"
    }

    static let rootElementID = "AlertViewController"
    static let rootElementName = "Алерт после нажатия на снять с продажи"
}
