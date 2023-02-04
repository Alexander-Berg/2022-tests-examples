import XCTest
import Snapshots

final class TradeInPickerOptionCell: BaseSteps, UIElementProvider {
    enum Element {
        case checkmark
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .checkmark: return "trade_in_option_checkmark"
        }
    }
}

final class TradeInPicker: BaseSteps, UIRootedElementProvider {
    enum Element {
        case tradeInOption(TradeInOption)
        case applyButton
        case skipButton
        case agreements
        case agreementsValidation

        enum TradeInOption: Int {
            case new = 3
            case used = 2
            case money = 1

            var name: String {
                switch self {
                case .new: return "новые"
                case .used: return "б/у"
                case .money: return "деньги"
                }
            }
        }
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .tradeInOption(let e): return "trade_in_option_\(e.rawValue)"
        case .applyButton: return "trade_in_apply_button"
        case .skipButton: return "trade_in_skip_button"
        case .agreements: return "trade_in_agreements"
        case .agreementsValidation: return "agreements_error_label"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .tradeInOption(let e): return "Опция трейд-ин `\(e.name)`"
        case .applyButton: return "Кнопка отправки"
        case .skipButton: return "Кнопка Пропустить"
        case .agreements: return "Блок соглашения"
        case .agreementsValidation: return "Ошибка об непринятом соглашении"
        }
    }

    static let rootElementID = "TradeInPickerViewController"
    static let rootElementName = "Продать дилеру"
}
