import XCTest

final class TextReasonPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case textView = "text_view"
        case confirm = "confirm_button"
    }

    static let rootElementName = "Попап ввода текста (причины снятия, отмены и т.д.)"
    static let rootElementID = "text_input_view_controller"
}
