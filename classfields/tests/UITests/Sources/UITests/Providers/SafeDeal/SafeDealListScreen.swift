import XCTest

final class SafeDealListScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case deal(String)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .deal(id): return "safe_deal_offer_\(id)"
        }
    }
    static let rootElementName = "Список безопасных сделок"
    static let rootElementID = "SafeDealListViewController"
}
