final class VASDescriptionContainerScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case closeButton
        case card(String)
    }

    static var rootElementID: String {
        "VASDescriptionContainerViewController"
    }

    static var rootElementName: String {
        "Экран с каруселью васов"
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .closeButton:
            return "close_button"

        case let .card(name):
            return "VASDescriptionViewController_\(name)"
        }
    }
}

final class VASDescriptionCard: BaseSteps, UIElementProvider {
    enum Element {
        case purchaseButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .purchaseButton:
            return "purchase_button"
        }
    }
}
