final class PaymentOptionsScreen_: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case purchaseButton = "purchase_button"
        case closeButton = "dismiss_modal_button"
    }

    static var rootElementID: String {
        "app.screen.payment_options"
    }

    static var rootElementName: String {
        "Экран выбора платежных опций"
    }
}
