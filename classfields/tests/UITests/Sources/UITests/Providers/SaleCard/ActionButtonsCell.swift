import Foundation

final class ActionButtonsCell: BaseSteps, UIElementProvider {
    enum Element {
        case safeDeal
        case share
        case note
        case compare
        case favorite
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .safeDeal: return "action_buttons_safe_deal"
        case .share: return "action_buttons_share"
        case .note: return "action_buttons_note"
        case .compare: return "action_buttons_compare"
        case .favorite: return "action_buttons_favorite"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .safeDeal: return "Сделка"
        case .share: return "Поделиться"
        case .note: return "Заметка"
        case .compare: return "Сравнить"
        case .favorite: return "Избранное"
        }
    }
}
