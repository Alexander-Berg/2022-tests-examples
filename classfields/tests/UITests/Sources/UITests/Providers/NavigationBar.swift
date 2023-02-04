import XCTest

final class NavigationBar: BaseSteps, UIRootedElementProvider {
    static let rootElementName = "Навбар"
    static let rootElementID = "NavBarView"

    enum Element: String {
        case title = "NavBarTitle"
        case back = "nav_back_button"
        case close = "nav_close_button"
        case superMenuButton = "super_menu_button"
    }

    func name(of element: Element) -> String {
        switch element {
        case .title:
            return "Заголовок навбара"
        case .back:
            return "Кнопка Назад в навбаре"
        case .close:
            return "Кнопка Закрыть в навбаре"
        case .superMenuButton:
            return "Кнопка открытия нового главного меню"
        }
    }
}
