final class UserSaleCardOfferVASCell: BaseSteps, UIElementProvider {
    enum Element {
        case buyButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .buyButton:
            return "buy_vas_button"
        }
    }
}
