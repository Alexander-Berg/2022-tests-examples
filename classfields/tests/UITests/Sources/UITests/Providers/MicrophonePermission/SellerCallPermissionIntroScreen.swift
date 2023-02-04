final class SellerCallPermissionIntroScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case allowButton
        case cancelButton
        case description
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .allowButton:
            return "Разрешить"

        case .cancelButton:
            return "Нет, спасибо"

        case .description:
            return "Так вы не упустите покупателей, которые предпочитают звонить через Авто.ру"
        }
    }

    static let rootElementID = "seller_call_permission_intro"
    static let rootElementName = "Шторка с предложением продавцу включить микрофон"
}
