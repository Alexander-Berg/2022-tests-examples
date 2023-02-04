import XCTest
import Snapshots

enum TabBarItem: String {
    case transport = "Поиск"
    case favorites = "Избранное"
    case offers = "Объявления"
    case offersAttentions = "Разместить"
    case chats = "Сообщения"
    case garage = "Гараж"
}

final class TabBar: BaseSteps, UIElementProvider {
    enum Element {
        case tab(TabBarItem)
    }

    func identifier(of element: Element) -> String {
        switch element {
        case let .tab(item):
            return item.rawValue
        }
    }
}
