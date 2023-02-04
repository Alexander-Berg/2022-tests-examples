import XCTest
import Snapshots

final class MainScreen_: BaseSteps, UIRootedElementProvider {
    enum Element {
        case navBarTab(NavBarTab)
        case tabBar
        case userOfferStatBubble
        case superMenuButton
    }

    enum NavBarTab: String {
        case transport = "ТРАНСПОРТ"
        case reports = "ПРОАВТО"
        case credits = "КРЕДИТЫ"
        case electrocars = "ЭЛЕКТРОМОБИЛИ"
        case insurance = "ОСАГО"
        case reviews = "ОТЗЫВЫ"
        case journal = "ЖУРНАЛ"
    }

    static var rootElementName = "Морда"
    static var rootElementID = "MainContainerViewController"

    let transport = contentItem(.transportScreen, with: .transport)
    let favorites = contentItem(.favoritesScreen, with: .favorites)
    let offers = contentItem(.userSaleListScreen, with: .offers)
    let offersAttentions = contentItem(.userSaleListScreen, with: .offersAttentions)
    let chats = contentItem(.chatsScreen, with: .chats)
    let garage = contentItem(.garageScreen, with: .garage)
    let garageLanding = contentItem(.garageLanding, with: .garage)

    func identifier(of element: Element) -> String {
        switch element {
        case .userOfferStatBubble:
            return "UserOfferStatView"
        case .tabBar:
            return "TabBarView"
        case .navBarTab(let value):
            return value.rawValue
        case .superMenuButton:
            return "super_menu_button"
        }
    }

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

extension MainScreen_: UIToggleContainer {
    typealias ContentID = TabBarItem

    func toggle(to tab: TabBarItem) {
        focus(on: .tabBar, ofType: .tabBar) {
            $0.tap(.tab(tab))
        }
    }
}
