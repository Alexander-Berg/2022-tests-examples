import XCTest

final class SafeDealCell: BaseSteps, UIElementProvider {
    enum Element: String {
        case call = "Позвонить"
        case chatWithBuyer = "Связаться с покупателем"
        case chatWithSeller = "Написать"
        case cancel = "Отменить запрос"
        case decline = "Отклонить"
        case accept = "Подтвердить"
        case deal = "Подробнее о сделке"
        case showOffer = "Перейти к объявлению"
        case sendRequest = "Отправить запрос"
    }
}
