import XCTest

final class TransportScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case userOfferStatBubble
        case storiesCarousel
        case creditPriceFilter
        case searchHistory(title: String)
        case filterParametersButton
        case safeDealStatusCell
    }

    static let rootElementName = "Таб Транспорт"
    static var rootElementID = "TransportViewController"

    func identifier(of element: Element) -> String {
        switch element {
        case .userOfferStatBubble:
            return "UserOfferStatView"
        case .storiesCarousel:
            return "stories"
        case .creditPriceFilter:
            return "Рассчитать кредит"
        case let .searchHistory(title):
            return title
        case .filterParametersButton:
            return "Параметры"
        case .safeDealStatusCell:
            return "safe_deal_status_cell"
        }
    }
}
