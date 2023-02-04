import XCTest

final class SafeDealRequestCancelPopup: BaseSteps, UIRootedElementProvider {
    enum Element {
        case reason(String)
        case close
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .reason(title): return title
        case .close: return "Закрыть"
        }
    }

    static let rootElementName = "Алерт почему решили отменить БС"
    static let rootElementID = "safe_deal_cancel_alert"
}
