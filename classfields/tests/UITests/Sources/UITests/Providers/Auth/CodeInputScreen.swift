import XCTest
import Snapshots

final class CodeInputScreen: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case codeInput = "codeInput"
        case invalidCodeError = "Неверный код подтверждения"
    }

    static let rootElementID = "code_confirmation_input"
    static let rootElementName = "Экран подтверждения телефона"
}
