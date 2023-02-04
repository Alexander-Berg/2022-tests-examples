import XCTest
import Snapshots
import AutoRuProtoModels

final class OfferPriceScreen: BaseScreen, Scrollable {
    lazy var scrollableElement = findAll(.collectionView).firstMatch
    lazy var popup = find(by: "ModalViewControllerHost").firstMatch

    func findItem(id: String) -> XCUIElement { find(by: id).firstMatch }

    func actionButton(_ type: ActionButton) -> XCUIElement { find(by: type.accessibilityID).firstMatch }

    enum ActionButton: String {
        case addToFavorites = "Следить за изменением цены"
        case removeFromFavorites = "Отписаться"
        case makeCall = "Позвонить"

        var accessibilityID: String {
            return "OfferPriceViewController.ActionButton"
        }
    }

    enum PriceState {
        /// обычная цена
        case normal(nds: Bool, discount: Bool, isDealer: Bool, greatDeal: GreatDeal?)
        /// кейс цена "от" + отображение без скидок рядом с валютной ценой
        case priceFrom(nds: Bool, discount: Bool, greatDeal: GreatDeal?, category: Auto_Api_Category)

        var title: String {
            switch self {
            case .normal(let nds, let discount, let isDealer, let gd):
                return "\(nds ? "с" : "без") НДС, \(discount ? "со скидками" : "без скидок"), "
                    + (isDealer ? "дилер" : "")
                    + ((gd?.title).flatMap({ "_\($0)" }) ?? "")
            case let .priceFrom(nds, discount, gd, _):
                return "цена 'от X руб.', \(nds ? "с" : "без") НДС, \(discount ? "со скидками" : "без скидок")"
                    + ((gd?.title).flatMap({ "_\($0)" }) ?? "")
            }
        }

        var id: String {
            switch self {
            case .normal(let nds, let discount, let isDealer, let gd):
                return "normal_\(nds ? "with_nds" : "without_nds")_\(discount ? "with_discount" : "without_discount")"
                    + (isDealer ? "_dealer" : "")
                    + ((gd?.rawValue).flatMap({ "_\($0)" }) ?? "")
            case let .priceFrom(nds, discount, gd, category):
                return "price_from_\(nds ? "with_nds" : "without_nds")_\(discount ? "with_discount" : "without_discount")"
                    + ((gd?.rawValue).flatMap({ "_\($0)" }) ?? "") + "_\(category)"
            }
        }

        static func priceFrom(nds: Bool, discount: Bool, greatDeal: GreatDeal?) -> PriceState {
            .priceFrom(nds: nds, discount: discount, greatDeal: greatDeal, category: .cars)
        }
    }

    enum GreatDeal: String {
        case good = "great_deal_good"
        case excellent = "great_deal_excellent"

        var title: String {
            switch self {
            case .good: return "хорошая цена"
            case .excellent: return "отличная цена"
            }
        }
    }
}
