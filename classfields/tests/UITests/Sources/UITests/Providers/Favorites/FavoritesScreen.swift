import XCTest

typealias FavoritesScreen_ = FavoritesSteps

extension FavoritesScreen_: UIRootedElementProvider {
    enum Element {
        case offer(OfferIdentifier)
        case label(_ text: String)
        case subscribeSwitch(id: String)
        case noOffersLabel
    }

    static let rootElementID = "favorites"
    static let rootElementName = "Избранное"

    func identifier(of element: Element) -> String {
        switch element {
        case let .offer(offerIdentifier):
            return "offer_\(offerIdentifier.id)"

        case let .label(text):
            return text

        case let .subscribeSwitch(id):
            return "favorite.savedfilters.subcribe.checkbox.\(id)"

        case .noOffersLabel:
            return "Сохраняйте понравившиеся объявления, узнавайте об изменении цен"
        }
    }
}
