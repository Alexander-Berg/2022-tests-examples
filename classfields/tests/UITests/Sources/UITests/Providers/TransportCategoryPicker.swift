import XCTest

final class TransportCategoryPicker: BaseSteps, UIRootedElementProvider {
    enum Element: String {
        case auto = "Легковые"
        case moto = "Мото"
        case commercial = "Комтранс"
    }

    static let rootElementID = "categoryPicker"
    static let rootElementName = "Пикер категории транспорта"
}
