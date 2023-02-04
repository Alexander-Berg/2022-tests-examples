import XCTest

final class SafeDealStatusCell: BaseSteps, UIElementProvider {
    enum Element {
        case title
        case cancel
        case link
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .title: return "safe_deal_status_title"
        case .cancel: return "safe_deal_status_cancel"
        case .link: return "safe_deal_status_link"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .title: return "Заголовок баннера-статуса БС"
        case .cancel: return "Кнопка отмена в баннере-статусе БС"
        case .link: return "Кнопка-ссылка в баннере-статусе БС"
        }
    }
}
