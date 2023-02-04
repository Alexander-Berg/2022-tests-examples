import XCTest
import Snapshots

final class DealerOfferActionScreen: BaseScreen {
    func button(_ button: Button) -> XCUIElement {
        return find(by: "actions_menu_view").descendants(matching: .cell).staticTexts[button.rawValue].firstMatch
    }

    enum Button: String, CaseIterable {
        case delete = "Удалить"
        case share = "Поделиться"
        case deactivate = "Снять с продажи"
        case activate = "Активировать"
        case edit = "Редактировать"
    }
}

final class DealerDeleteConfirmationScreen: BaseScreen {
    lazy var cancelButton = find(by: "Отмена").firstMatch
    lazy var deleteButton = find(by: "Удалить").firstMatch
}
