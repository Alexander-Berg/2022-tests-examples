import Foundation

final class TradeInOfferPickerScreen: BaseSteps, UIRootedElementProvider {
    static let rootElementID = "trade_in_offer_picker_screen"
    static let rootElementName = "Пикер выбора оффера юзера для трейд-ин из карточки продавца"

    enum Element {
        case tradeInUserOffer
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .tradeInUserOffer:
            return "trade_in_user_offer"
        }
    }
}
