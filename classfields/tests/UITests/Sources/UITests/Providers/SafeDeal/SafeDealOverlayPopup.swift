import XCTest

final class SafeDealOverlayPopup: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case close = "dismiss_modal_button"
        case understand = "understand_button"
        case details = "details_button"
    }

    static let rootElementName = "Паранжа в оффере с подсветкой кнопки Сделка"
    static let rootElementID = "BlurOverlayViewController"
}
