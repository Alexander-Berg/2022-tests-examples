final class OfferAdvantagesModalPopup: BaseSteps, UIRootedElementProvider {
    enum Element {
        case electrocarsButton
        case reportButton
        case reviewsButton
        case chatButton
        case callButton
        case provenOwnerButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .electrocarsButton:
            return "electrocarButton"
        case .reportButton:
            return "reportButton"
        case .reviewsButton:
            return "reviewsButton"
        case .chatButton:
            return "chatButton"
        case .callButton:
            return "callButton"
        case .provenOwnerButton:
            return "provenOwnerButton"
        }
    }

    static let rootElementID = "OfferAdvantagesViewController"
    static let rootElementName = "Попап преимуществ карточки"
}
