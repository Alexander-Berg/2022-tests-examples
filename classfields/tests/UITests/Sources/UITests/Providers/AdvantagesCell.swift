final class AdvantagesCell: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case electrocarsAdvantage = "electrocar"
        case onlineViewAdvantage = "online_view_available"
        case provenOwnerAdvantage = "proven_owner"
        case noAccidentsAdvantage = "no_accidents"
        case oneOwnerAdvantage = "one_owner"
        case certificateManufacturerAdvantage = "certificate_manufacturer"
        case warrantyAdvantage = "warranty"
        case almostNewAdvantage = "almost_new"
        case highReviewsMarkAdvantage = "high_reviews_mark"
        case stablePriceAdvantage = "stable_price"
    }

    static let rootElementID = "advantages"
    static let rootElementName = "Преимущества на карточке"
}
