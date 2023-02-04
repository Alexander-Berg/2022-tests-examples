import XCTest

final class AuctionPreOffersScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case nextButton
        case preOffer(dealerName: String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .nextButton: return "submit_button"
        case .preOffer(let name): return "preoffer_\(name)"
        }
    }

    static var rootElementName = "Экран со списком дилерских предложений аукциона"
    static var rootElementID = "dealers_buyback_list"
}
