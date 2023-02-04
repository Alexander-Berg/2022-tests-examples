import Foundation
import XCTest

final class FeedbackModalScreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case emailField
        case messageField
        case submitButton
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .emailField: return "emailField"
        case .messageField: return "messageField"
        case .submitButton: return "submitButton"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .emailField: return "Поле ввода email"
        case .messageField: return "Поле ввода сообщения"
        case .submitButton: return "Кнопка отправки"
        }
    }

    static let rootElementID = "FeedbackModalView"
    static let rootElementName = "Модалка написания отзыва на приложение"
}
