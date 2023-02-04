import Foundation
import XCTest

final class ReviewCardScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case specificationsButton
        case offersButton
        case commentsButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .specificationsButton: return "specifications_button"
        case .offersButton: return "offers_button"
        case .commentsButton: return "comments_button"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .specificationsButton: return "Кнопка Все характеристики"
        case .offersButton: return "Кнопка Объявления"
        case .commentsButton: return "Кнопка Комментарии"
        }
    }

    static let rootElementID = "review_card_screen"
    static let rootElementName = "Карточка отзыва"
}
